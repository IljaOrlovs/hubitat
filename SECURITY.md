# Security

Report vulnerabilities privately via GitHub's "Report a vulnerability" (Security tab), not a public issue.

These drivers talk to devices on your LAN and hold API tokens/passwords in Hubitat preferences. Never commit credentials; use `input type: "password"` for secrets. Known credential-handling gaps are listed in `AGENTS.md`.
