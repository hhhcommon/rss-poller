# rss-poller
Polls RSS feeds, converts HTML entries to PDF &amp; stores on FS


# RSS Poller

The RSS Poller is a configurable standalone Spring Boot CLI Application utilizing Spring Integration channels that will (when executed) follow the following process:
1. Poll the configured RSS feeds for new items
1. Filter through the XML objects (in POJO form) and find the links to the reports.
1. Download the reports, hash the file & compare them to a list of previously hashed items (SI does this for us)
1. If the item is not found in the previous list, then submit a message to the appropriate output channel

