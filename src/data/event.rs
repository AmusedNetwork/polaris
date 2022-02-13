//! Database models related to events

use anyhow::Result;
use serde::{Deserialize, Serialize};
use sqlx::types::chrono::{DateTime, Utc};
use sqlx::PgPool;

/// Database model for an Event
#[derive(Debug, Deserialize, Serialize)]
pub struct Event {
    /// The database id
    pub id: Option<i32>,
    /// The name of the event
    pub name: String,
    /// When the event starts
    pub start_time: Option<DateTime<Utc>>,
    /// When the event ends
    pub end_time: Option<DateTime<Utc>>,
    /// The max CPU time allocated for this event's challenges
    pub max_cpu: Option<i64>,
    /// The max RAM allocated for this event's challenges (in bytes)
    pub max_ram: Option<i64>,
    /// The URL of this event's instance of RACTF Core
    pub api_url: Option<String>,
    /// An API token for interacting with this event's instance of RACTF Core
    pub api_token: Option<String>,
}

impl Event {
    /// Insert or update this event into the database
    pub async fn save(&mut self, pool: &PgPool) -> Result<()> {
        let result = sqlx::query!("SELECT id FROM event WHERE id=$1", self.id)
            .fetch_optional(pool)
            .await?;
        if result.is_none() {
            let insert = sqlx::query!(r#"
                INSERT INTO event ( name, start_time, end_time, max_cpu, max_ram, api_url, api_token )
                VALUES ($1, $2, $3, $4, $5, $6, $7)
                RETURNING id
            "#,
                self.name, self.start_time, self.end_time, self.max_cpu, self.max_ram, self.api_url, self.api_token
            ).fetch_one(pool).await?;
            self.id = Some(insert.id);
        } else {
            sqlx::query!(r#"
                UPDATE event SET name = $1, start_time = $2, end_time = $3, max_cpu = $4, max_ram = $5, api_url = $6, api_token = $7 WHERE id = $8
            "#,
                self.name, self.start_time, self.end_time, self.max_cpu, self.max_ram, self.api_url, self.api_token, self.id
            ).execute(pool).await?;
        }
        Ok(())
    }

    /// Get an event from a given id
    pub async fn get(pool: &PgPool, id: i32) -> Result<Event> {
        let result = sqlx::query!("SELECT id, name, start_time, end_time, max_cpu, max_ram, api_url, api_token FROM event WHERE id=$1", id).fetch_one(pool).await?;
        //TODO: https://github.com/launchbadge/sqlx/issues/367
        Ok(Event {
            id: Some(result.id),
            name: result.name,
            start_time: result.start_time,
            end_time: result.end_time,
            max_cpu: result.max_cpu,
            max_ram: result.max_ram,
            api_url: result.api_url,
            api_token: result.api_token,
        })
    }

    /// Delete this event
    pub async fn delete(&mut self, pool: &PgPool) -> Result<()> {
        sqlx::query!("DELETE FROM event WHERE id=$1", self.id)
            .execute(pool)
            .await?;
        Ok(())
    }

    /// Delete an event with a given id
    pub async fn delete_id(pool: &PgPool, id: i32) -> Result<()> {
        sqlx::query!("DELETE FROM event WHERE id=$1", id)
            .execute(pool)
            .await?;
        Ok(())
    }

    /// Check if an event name is taken
    pub async fn is_name_taken(pool: &PgPool, name: &String) -> Result<bool> {
        let result = sqlx::query!("SELECT id FROM event WHERE name=$1", name)
            .fetch_optional(pool)
            .await?;
        Ok(result.is_some())
    }

    /// Get all events in the database
    pub async fn get_all(pool: &PgPool) -> Result<Vec<Event>> {
        let results = sqlx::query!("SELECT id, name, start_time, end_time, max_cpu, max_ram, api_url, api_token FROM event").fetch_all(pool).await?;
        //TODO: https://github.com/launchbadge/sqlx/issues/367
        let mut events = Vec::with_capacity(results.len());
        for result in results {
            events.push(Event {
                id: Some(result.id),
                name: result.name,
                start_time: result.start_time,
                end_time: result.end_time,
                max_cpu: result.max_cpu,
                max_ram: result.max_ram,
                api_url: result.api_url,
                api_token: result.api_token,
            });
        }
        Ok(events)
    }

    /// Check if an event id is stored in the database
    pub async fn id_exists(pool: &PgPool, id: i32) -> Result<bool> {
        let result = sqlx::query!("SELECT id FROM event WHERE id=$1", id)
            .fetch_optional(pool)
            .await?;
        Ok(result.is_some())
    }
}