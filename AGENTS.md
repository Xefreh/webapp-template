# Agent instructions

## Editing files

- Create and modify files through the `eclipse` MCP server, not with the agent's built-in file tools or shell commands (`sed`, heredocs, ...):
  - `eclipse_edit_file` to change part of an existing file;
  - `eclipse_write_file` to create a file, or to rewrite one entirely (`overwrite: true`).
- Paths are Eclipse workspace paths, whose first segment is the project name:
  - `/app/...` for everything under `app/` (e.g. `/app/src/main/java/org/example/App.java`);
  - `/webapp-template/...` for files at the repository root (e.g. `/webapp-template/gradle/libs.versions.toml`).
- Run `eclipse_format` on Java files after editing them, and check `eclipse_get_problems` for compilation errors.
- `eclipse_format` only changes the editor buffer when the file is open in Eclipse, without saving it to disk. After formatting:
  1. call `eclipse_list_editors` and make sure the only dirty editors are the files you just formatted (never save the user's unsaved work);
  2. save them with `eclipse_run_workbench_command` and the command `org.eclipse.ui.file.saveAll`;
  3. check that the change reached the disk (`git diff`) before committing.
- Fall back to the built-in tools only if the Eclipse MCP server is unavailable, and say so.
