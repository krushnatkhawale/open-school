#!/bin/zsh
# Local launch wrapper for the Private Digital School app.
# Sources TELEGRAM_BOT_TOKEN from ~/.zshrc so the secret is not duplicated here.
set -e
cd /Users/hulk/.buzz/REPOS/open-school
source "$HOME/.zshrc" 2>/dev/null
export JAVA_HOME="${JAVA_HOME:-/Users/hulk/Library/Java/JavaVirtualMachines/openjdk-25/Contents/Home}"
exec "$JAVA_HOME/bin/java" \
  -jar build/libs/my-private-digital-school-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=local
