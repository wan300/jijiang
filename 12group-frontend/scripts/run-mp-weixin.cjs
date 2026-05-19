const { spawn, spawnSync } = require("child_process");
const path = require("path");

const mode = process.argv[2] === "build" ? "build" : "dev";
const projectRoot = path.resolve(__dirname, "..");
const configScript = path.join(__dirname, "weixin-project-config.cjs");
const uniBin = path.join(projectRoot, "node_modules", ".bin", process.platform === "win32" ? "uni.cmd" : "uni");
const uniArgs = mode === "build" ? ["build", "-p", "mp-weixin"] : ["-p", "mp-weixin"];

let syncedAfterCompile = false;
let outputBuffer = "";

function syncConfig() {
  const result = spawnSync(process.execPath, [configScript, mode], {
    cwd: projectRoot,
    stdio: "inherit",
  });
  if (result.status !== 0) {
    process.exit(result.status || 1);
  }
}

function syncAfterCompile(text) {
  outputBuffer = `${outputBuffer}${text}`.slice(-4000);
  if (syncedAfterCompile) return;
  if (!/DONE\s+Build complete|ready in/i.test(outputBuffer)) return;
  syncedAfterCompile = true;
  for (const delay of [500, 2000, 5000]) {
    setTimeout(syncConfig, delay);
  }
}

syncConfig();

const child = spawn(uniBin, uniArgs, {
  cwd: projectRoot,
  stdio: ["inherit", "pipe", "pipe"],
  shell: process.platform === "win32",
});

child.stdout.on("data", (chunk) => {
  process.stdout.write(chunk);
  syncAfterCompile(chunk.toString());
});

child.stderr.on("data", (chunk) => {
  process.stderr.write(chunk);
});

child.on("exit", (code, signal) => {
  syncConfig();
  if (signal) {
    process.kill(process.pid, signal);
    return;
  }
  process.exit(code || 0);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    child.kill(signal);
  });
}
