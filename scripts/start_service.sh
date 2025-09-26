#!/usr/bin/env bash
set -euo pipefail
systemctl daemon-reload || true
systemctl start c2-agent.service
systemctl enable c2-agent.service || true