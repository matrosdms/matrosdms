# Google Tasks & Calendar Sync

MatrosDMS can push your tasks (actions) to **Google Tasks**, and optionally their due dates to an
extra **Google calendar**. Ticking a task off or moving its date in Google comes back to
MatrosDMS. It is off until you set it up. Without the OAuth credentials described below,
MatrosDMS never contacts Google.

- **Per user:** every MatrosDMS user connects their own Google account. A task is pushed to
  the account of its **assignee**.
- **In the background:** saving a task never waits for Google. The sync runs as a persistent
  job a few seconds later and is retried while you are offline.
- **Two-way for what matters on the go:** completion and due dates. Everything else (title,
  description, priority, assignment) is owned by MatrosDMS and overwritten in Google with the
  next change.

> **Why the calendar is optional.** Google Calendar already shows your Google Tasks on their due
> date, and you can tick them off or drag them to another day right there. So connecting only
> asks for Google Tasks. The extra "MatrosDMS" calendar is off until you switch it on, and only
> then does Google ask for that permission. It is worth it if you want the dates in other calendar
> apps (iPhone, Outlook), want to share them with someone, or want event reminders. With both on,
> every task appears twice in Google Calendar: once as a task and once as an event.

## What ends up where

| In MatrosDMS | Google Tasks (list "MatrosDMS") | Optional: Google Calendar (calendar "MatrosDMS") |
|---|---|---|
| Open task | Task, due date (date only) | All-day event on the due date, if it has one |
| Marked Done / Rejected | Ticked off | Event removed |
| Reopened | Unticked | Event back |
| Due date removed | Due date cleared | Event removed |
| Deleted | Task deleted | Event deleted |
| Reassigned | Moves to the new assignee's account, or is removed if they have not connected | same |

The task notes and event description contain the description, the resolution (once done), the
linked document and context names, and the priority if it is not "Normal".

When you connect, all your **open** tasks are pushed. Tasks that were already finished are not.

### Changes made in Google

| In Google | In MatrosDMS |
|---|---|
| Task ticked off (Tasks app, Calendar, Gmail side panel) | Status **Done**, history entry "Completed in Google Tasks" |
| Task unticked | Status **Open** again |
| Task due date changed | Due date changed. The date comes from Google, the time of day is kept |
| Calendar event moved | Due date changed to the event's start |
| Due date removed | Due date removed |
| Task or event **deleted** | Nothing. The task stays in MatrosDMS and is recreated in Google with its next change |
| Title or notes edited | Not taken over. MatrosDMS writes them and overwrites them with the next change |

MatrosDMS checks Google for such changes every 2 minutes while it is running, and asks only
for what changed since the last check: one small request per list or calendar. Changes made
while MatrosDMS was not running are picked up at the next start. Open task lists in the browser
reload by themselves and show a notice. Change the interval with `MATROS_GOOGLE_POLL_INTERVAL`
(e.g. `5m`).

**Conflicts.** MatrosDMS remembers what it last synced. A field counts as changed in Google
only if it differs from that, so its own updates are never mistaken for yours. If the same
field was changed on both sides before a sync, the later change wins. Before every push,
MatrosDMS first reads the copy in Google, so a push never overwrites a change made in Google
that was not fetched yet.

The optional calendar is a separate "MatrosDMS" calendar that MatrosDMS creates itself. The
permission it asks for (`calendar.app.created`) only covers calendars it created, so it cannot
see your other calendars. You can hide it, recolour it or delete it in Google Calendar like any other
calendar. Events are marked "free", so they do not block your availability.

## Setup

### 1. Create a Google Cloud project (once, a few minutes)

1. Open <https://console.cloud.google.com/> and create a project, e.g. "MatrosDMS".
2. **APIs & Services → Library**: enable **Google Tasks API** and **Google Calendar API**.
3. **Google Auth Platform → Branding**: enter an app name ("MatrosDMS") and your email.
4. **Audience**: choose **External**, then click **Publish app** so the status is
   **In production**.
   > While the app is in *Testing*, Google lets every refresh token expire after **7 days**,
   > so the sync would stop every week. In production, an app you did not have verified by
   > Google works for up to 100 users. Google shows a "Google hasn't verified this app" screen
   > once while you connect: click **Advanced → Go to MatrosDMS**.
5. **Clients → Create client**, application type **Desktop app**. Copy the **client ID** and
   the **client secret**.

### 2. Give MatrosDMS the credentials

Set two environment variables before MatrosDMS starts, for example in `startMatros.cmd`:

```bat
set MATROS_GOOGLE_CLIENT_ID=1234567890-abc.apps.googleusercontent.com
set MATROS_GOOGLE_CLIENT_SECRET=GOCSPX-...
```

For the packaged `MatrosDMS.exe` you can set them as Windows user environment variables
(`setx MATROS_GOOGLE_CLIENT_ID ...`), or put a `config/application.yaml` into the folder
MatrosDMS is started from:

```yaml
app:
  integrations:
    google:
      client-id: 1234567890-abc.apps.googleusercontent.com
      client-secret: GOCSPX-...
```

One client works for all tenant profiles and ports: a Desktop client accepts any
`localhost` port. Do not commit the secret to a repository.

### 3. Connect

In MatrosDMS open **My Profile → Google Tasks & Calendar → Connect Google**, sign in and
allow access to Google Tasks. You come back to your profile, and your open tasks show up in
Google within a few seconds.

The two switches turn Tasks and the extra calendar on or off separately. Switching the calendar
on the first time takes you to Google once more, to allow that one permission. Turning a switch
off removes what MatrosDMS put there.

## Offline, errors and retries

- Each change becomes a job in the database (the same db-scheduler that runs indexing). It
  survives a restart.
- If Google cannot be reached, the job is retried after 30 s, 1 min, 2 min, … and then hourly,
  for about two days. After that the task syncs again with its next change, or when you press
  **Sync now**.
- Problems that no retry fixes are shown on **My Profile** and are not retried: an API that is
  not enabled, a wrong client secret, or access you revoked.

| Message | Fix |
|---|---|
| `Google Tasks API has not been used in project … or it is disabled` | Enable the API (step 1.2) |
| `invalid_client` | Client ID or secret is wrong |
| `Google access was revoked or has expired` | Connect again. In *Testing* mode this happens weekly (step 1.4) |
| `redirect_uri_mismatch` on Google's page | The client is not of type *Desktop app*, or see "Server installs" below |
| A switch jumps back to off after Google's page | You did not tick that permission there. Switch it on again and allow it |

## Disconnect

**Disconnect** revokes MatrosDMS's access and pauses the sync. Nothing is deleted in Google.
If you connect the **same** Google account again, MatrosDMS carries on with the existing list,
calendar and tasks, without duplicates. If you connect a **different** account, MatrosDMS
starts fresh there; the copies in the old account stay.

## Privacy

Sent to Google: task title, description, resolution, due date, priority, and the names of the
linked document and context. Documents themselves are never sent.

The Google refresh token is stored in the MatrosDMS database, encrypted with AES-256-GCM under
a key derived from the store key (`MATROS_STORE_PASSWORD`). If you change the store password,
connect Google again. With store encryption switched off, the token is stored unencrypted and
the log says so.

## Server installs (Docker, Helm)

A *Desktop app* client only works when the browser opens MatrosDMS on `localhost` or
`127.0.0.1`. For a server reached under a host name:

1. Create a client of type **Web application** and add the authorized redirect URI
   `https://<your-host>/api/integrations/google/callback`. Google requires https for anything
   other than localhost.
2. Set `MATROS_GOOGLE_REDIRECT_URI` to exactly that URL, in addition to the client ID and
   secret.

## For plugin developers

The `ActionSyncPlugin` SPI (JARs in `workspace/plugins`) still exists, alongside the built-in
Google sync. Its callbacks run **after the commit**, on one background thread, in order. They
never block a request any more, but they are best effort: a plugin that needs retries has to
handle them itself.
