const fs = require("fs");
const path = require("path");

const mode = process.argv[2] === "dev" ? "dev" : "build";
const projectRoot = path.resolve(__dirname, "..");
const miniprogramRoot = `dist/${mode}/mp-weixin/`;
const rootConfigPath = path.join(projectRoot, "project.config.json");
const outputConfigPath = path.join(projectRoot, miniprogramRoot, "project.config.json");
const outputConfigDir = path.dirname(outputConfigPath);

const defaultSetting = {
  urlCheck: false,
  es6: true,
  postcss: true,
  minified: false,
  newFeature: true,
  bigPackageSizeSupport: true,
};

function readJson(file) {
  try {
    return JSON.parse(fs.readFileSync(file, "utf8"));
  } catch {
    return {};
  }
}

function writeJson(file, data) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(data, null, 2)}\n`);
}

function loadDotEnv() {
  const envPath = path.join(projectRoot, ".env");
  if (!fs.existsSync(envPath)) return;
  const lines = fs.readFileSync(envPath, "utf8").split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const index = trimmed.indexOf("=");
    if (index <= 0) continue;
    const key = trimmed.slice(0, index).trim();
    if (process.env[key] !== undefined) continue;
    process.env[key] = trimmed.slice(index + 1).trim();
  }
}

loadDotEnv();

const existing = readJson(rootConfigPath);
const mpApiBase = process.env.VITE_MP_API_BASE || "http://localhost:8080";
const config = {
  ...existing,
  compileType: "miniprogram",
  miniprogramRoot,
  appid: process.env.WX_MINIPROGRAM_APPID || existing.appid || "wx7961fa27f4fced94",
  projectname: existing.projectname || "12group-frontend",
  setting: {
    ...defaultSetting,
    ...(existing.setting || {}),
  },
};

writeJson(rootConfigPath, config);

fs.mkdirSync(outputConfigDir, { recursive: true });

const outputConfig = {
  ...config,
};
delete outputConfig.miniprogramRoot;
writeJson(outputConfigPath, outputConfig);

console.log(`[weixin-project-config] mode=${mode}`);
console.log(`[weixin-project-config] miniprogramRoot=${miniprogramRoot}`);
console.log(`[weixin-project-config] VITE_MP_API_BASE=${mpApiBase}`);

if (!/^https?:\/\//i.test(mpApiBase.trim())) {
  console.warn("[weixin-project-config] WARNING: VITE_MP_API_BASE must be an absolute http(s) URL for mp-weixin requests.");
}
