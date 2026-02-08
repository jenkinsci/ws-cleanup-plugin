# CLAUDE.md

Clean Jenkins workspaces before or after builds with pattern-based file selection.

## Stack
- Java
- Maven
- Jenkins 2.479.3+
- Resource Disposer plugin for deferred cleanup

## Build & Test
```bash
mvn clean verify
mvn hpi:run
```

## Notes
- Ant pattern syntax for include/exclude rules
- Deferred wipeout for performance (delegates to resource-disposer)
- Pipeline step: `cleanWs()`
- Freestyle: build wrapper and post-build action
- Can be disabled per-node via `DisableDeferredWipeoutNodeProperty`
