# Security policy

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Use GitHub private
vulnerability reporting in the repository Security tab.

Never include production credentials, customer information, precise location
history, access tokens, or signed APKs in a report. Use synthetic identifiers
and the smallest reproducible example.

## Public repository guarantees

The public repository must remain unable to produce a connected production APK
without configuration held by the private release process. Every pull request
must pass the public-boundary scan and the disconnected build gate.
