package uk.co.ractf.polaris.controller;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.ractf.polaris.api.challenge.Challenge;
import uk.co.ractf.polaris.api.deployment.Deployment;
import uk.co.ractf.polaris.api.instance.Instance;
import uk.co.ractf.polaris.controller.task.ControllerServices;
import uk.co.ractf.polaris.host.Host;
import uk.co.ractf.polaris.instanceallocation.EphemeralInstanceAllocator;
import uk.co.ractf.polaris.instanceallocation.InstanceAllocator;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class EphemeralController implements Controller, Managed {

    private static final Logger log = LoggerFactory.getLogger(EphemeralController.class);

    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();
    private final Map<String, Deployment> deployments = new ConcurrentHashMap<>();
    private final Multimap<String, Instance> deploymentInstances = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final Map<String, Semaphore> deploymentLocks = new ConcurrentHashMap<>();
    private final Map<String, Instance> instances = new ConcurrentHashMap<>();
    private final Map<String, Host> hosts = new ConcurrentHashMap<>();
    private final Set<Service> services;
    private final InstanceAllocator instanceAllocator = new EphemeralInstanceAllocator(this);

    @Inject
    public EphemeralController(@ControllerServices final Set<Service> services) {
        this.services = services;
    }

    @Override
    public void start() {
        for (final Service service : services) {
            service.startAsync();
        }
    }

    @Override
    public void stop() {
        for (final Service service : services) {
            service.stopAsync();
        }
    }

    @Override
    public void addHost(final Host host) {
        hosts.put(host.getID(), host);
    }

    @Override
    public Map<String, Challenge> getChallenges() {
        return Collections.unmodifiableMap(challenges);
    }

    @Override
    public Challenge getChallenge(final String id) {
        return challenges.get(id);
    }

    @Override
    public void submitChallenge(final Challenge challenge) {
        challenges.put(challenge.getID(), challenge);
    }

    @Override
    public void deleteChallenge(final String id) {
        challenges.remove(id);
    }

    @Override
    public Map<String, Deployment> getDeployments() {
        return Collections.unmodifiableMap(deployments);
    }

    @Override
    public Deployment getDeployment(final String id) {
        return deployments.get(id);
    }

    @Override
    public void createDeployment(final Deployment deployment) {
        deployments.put(deployment.getID(), deployment);
    }

    @Override
    public void updateDeployment(final Deployment deployment) {
        deployments.put(deployment.getID(), deployment);
    }

    @Override
    public void deleteDeployment(final String id) {
        deployments.remove(id);
        CompletableFuture.runAsync(() -> {
            final Collection<Instance> instanceList = deploymentInstances.get(id);
            for (final Instance instance : instanceList) {
                hosts.get(instance.getHostID()).removeInstance(instance);
                instances.remove(instance.getID());
            }
            deploymentInstances.removeAll(id);
        });
    }

    @Override
    public Challenge getChallengeFromDeployment(final String deploymentID) {
        final Deployment deployment = deployments.get(deploymentID);
        return deployment == null ? null : getChallengeFromDeployment(deployment);
    }

    @Override
    public Challenge getChallengeFromDeployment(final Deployment deployment) {
        return challenges.get(deployment.getChallenge());
    }

    @Override
    public Map<String, Host> getHosts() {
        return Collections.unmodifiableMap(hosts);
    }

    @Override
    public Host getHost(final String id) {
        return hosts.get(id);
    }

    @Override
    public @NotNull List<Deployment> getDeploymentsOfChallenge(final String challenge) {
        final List<Deployment> deployments = new ArrayList<>();
        for (final Deployment deployment : this.deployments.values()) {
            if (deployment.getChallenge().equals(challenge)) {
                deployments.add(deployment);
            }
        }

        return deployments;
    }

    @Override
    public @NotNull List<Instance> getInstancesForDeployment(final String deployment) {
        return new ArrayList<>(deploymentInstances.get(deployment));
    }

    @Override
    public InstanceAllocator getInstanceAllocator() {
        return instanceAllocator;
    }

    @Override
    public Instance getInstance(final String id) {
        return instances.get(id);
    }

    @Override
    public void registerInstance(final Deployment deployment, final Instance instance) {
        instances.put(instance.getID(), instance);
        deploymentInstances.put(deployment.getID(), instance);
    }

    @Override
    public void unregisterInstance(final Deployment deployment, final Instance instance) {
        instances.remove(instance.getID());
    }

    @Override
    public boolean lockDeployment(final Deployment deployment) {
        try {
            deploymentLocks.computeIfAbsent(deployment.getID(), x -> new Semaphore(1)).acquire();
        } catch (final InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean unlockDeployment(final Deployment deployment) {
        deploymentLocks.computeIfAbsent(deployment.getID(), x -> new Semaphore(1)).release();
        return true;
    }

    /*private void reconciliationTick() {
        log.trace("Running controller reconciliation tick");
        for (final String deploymentID : deployments.keySet()) {
            CompletableFuture.runAsync(() -> {
                final Deployment deployment = deployments.get(deploymentID);
                final Challenge challenge = challenges.get(deployment.getChallenge());
                final SemaphoreInstanceList semaphoreInstanceList = deploymentInstances.computeIfAbsent(deploymentID, x -> new SemaphoreInstanceList());
                if (semaphoreInstanceList.isBusy()) {
                    return;
                }
                final List<Instance> deploymentInstances = semaphoreInstanceList.getInstances();
                final int scaleAmount = ReplicationController.create(deployment.getReplication()).getScaleAmount(deploymentInstances, this);
                if (scaleAmount > 0) {
                    for (int i = 0; i < scaleAmount; i++) {
                        CompletableFuture.runAsync(() -> {
                            final Host host = scheduler.scheduleChallenge(challenge, hosts.values());
                            final Instance instance = host.createInstance(challenge, deployment);
                            deploymentInstances.add(instance);
                            instances.put(instance.getID(), instance);
                            log.info("Scheduled instance {}(challenge: {}, deployment: {}) onto {}", instance.getID(), challenge.getID(), deployment.getID(), host.getID());
                        });
                    }
                }
                semaphoreInstanceList.free();
            });
        }
    }*/

}
