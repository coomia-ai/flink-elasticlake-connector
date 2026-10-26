# Security Policy

English | [简体中文](SECURITY.zh-CN.md)

## Supported Versions

There are no released or production-supported versions. The current scaffold is
under active development; fixes target the development branch until a release
policy is published.

## Report a Vulnerability

Do not disclose exploitable details or credentials in a public issue. On GitHub,
use **Security > Report a vulnerability** if maintainers have enabled private
reporting. Otherwise use a verified private contact on a maintainer's hosting
profile. No security email address or response SLA has been configured yet;
maintainers must establish a working private channel before public launch.

Include the affected commit/version, reproduction, impact, deployment context,
and a possible mitigation. Remove secrets and personal data. Coordinate disclosure
with the maintainers after a fix or mitigation is available.

## Development Expectations

Keep credentials outside source control and job configuration examples. Preserve
TLS verification. Validate and bound serialized data before allocating resources.
Dependency, recovery-state, and request-integrity changes need explicit review.
See the [architecture](docs/architecture.md) for the planned trust boundaries.
