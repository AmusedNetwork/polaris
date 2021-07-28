package uk.co.ractf.polaris.notification;

import uk.co.ractf.polaris.api.notification.Notification;
import uk.co.ractf.polaris.api.notification.NotificationReceiver;
import uk.co.ractf.polaris.api.notification.SlackWebhook;

public interface Notifier {

    void notify(final Notification notification);

    static Notifier from(final NotificationReceiver receiver) {
        if (receiver instanceof SlackWebhook) {
            return new SlackNotifier((SlackWebhook) receiver);
        }
        return null;
    }

}
