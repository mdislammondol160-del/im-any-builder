import fs from "node:fs";

import path from "node:path";

import { execFileSync } from "node:child_process";



const source = process.env.SOURCE_ARCHIVE ?? "";

const outputDir = path.resolve("app/src/main/assets");

const staging = path.resolve(".worker-staging");

const maxBytes = 25 * 1024 * 1024;



fs.rmSync(staging, { recursive: true, force: true });

fs.mkdirSync(staging, { recursive: true });

fs.mkdirSync(outputDir, { recursive: true });



const writeFallback = () => {
  
  fs.writeFileSync(path.join(outputDir, "index.html"), `<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>IM Any Builder</title></head><body style="font-family:Arial;text-align:center;padding:3rem"><h1>IM Any Builder</h1><p>No source payload was supplied for this build.</p></body></html>`);
  
};



if (!source) {
  
  writeFallback();
  
  process.exit(0);
  
}



const inputPath = path.resolve(source);

if (!fs.existsSync(inputPath) || fs.statSync(inputPath).size > maxBytes) {
  
  throw new Error("Source archive is missing or exceeds the 25 MB worker limit");
  
}



const entries = execFileSync("unzip", ["-Z1", inputPath], { encoding: "utf8" })

  .split("\n")

  .map((entry) => entry.trim())

  .filter(Boolean);



for (const entry of entries) {
  
  const normalized = path.posix.normalize(entry.replaceAll("\\", "/"));
  
  if (normalized.startsWith("../") || normalized.includes("/../") || normalized.startsWith("/")) {
    
    throw new Error(`Unsafe ZIP entry rejected: ${entry}`);
    
  }
  
}



const htmlEntry = entries.find((entry) => path.posix.basename(entry).toLowerCase() === "index.html")

  ?? entries.find((entry) => entry.toLowerCase().endsWith(".html"));

if (!htmlEntry) throw new Error("The source archive does not contain an HTML entry");



execFileSync("unzip", ["-q", "-o", inputPath, htmlEntry, "-d", staging]);

const extractedPath = path.join(staging, htmlEntry);

const html = fs.readFileSync(extractedPath, "utf8");

if (!/<html[\s>]/i.test(html) && !/<!doctype\s+html/i.test(html)) {
  
  throw new Error("Selected source entry is not valid HTML");
  
}



fs.writeFileSync(path.join(outputDir, "index.html"), html);















