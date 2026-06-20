# `onboarding-mcp-server`

This folder is **the project you create** by following the root **[`README.md`](../README.md)** (start at Step 1). On a minimal clone you might only have **`rideflex-docs/`** and that guide at first — that is normal.

**Build and run** (from inside this directory, after you have created the files from the guide):

```bash
mvn clean package -DskipTests
java -Dapp.docs.path="$(pwd)/../rideflex-docs" -jar target/onboarding-mcp-server-0.0.1-SNAPSHOT.jar
```

Logs: **`/tmp/onboarding-mcp-server.log`** — keep the terminal quiet for STDIO MCP.
