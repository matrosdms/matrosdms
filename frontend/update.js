import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// Setup __dirname for ES Modules
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 1. Look for patch.xml
const xmlPath = path.join(__dirname, 'patch.xml');

if (!fs.existsSync(xmlPath)) {
    console.error('❌ Error: patch.xml not found in this directory.');
    console.log('   (Make sure you saved the XML code from the previous step as "patch.xml")');
    process.exit(1);
}

const xmlContent = fs.readFileSync(xmlPath, 'utf8');

// 2. Regex Parser
const fileRegex = /<file path="(.*?)"><!\[CDATA\[([\s\S]*?)\]\]><\/file>/g;

let match;
let count = 0;

console.log('🛠  Applying Patch...');

while ((match = fileRegex.exec(xmlContent)) !== null) {
    const filePath = match[1];
    const fileContent = match[2].trim();
    
    // Resolve path relative to script location
    const targetPath = path.join(__dirname, filePath);
    const dir = path.dirname(targetPath);
    
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    fs.writeFileSync(targetPath, fileContent);
    console.log(`   📝 Updated: ${filePath}`);
    count++;
}

console.log('-----------------------------------------');
console.log(`✅ Success! Processed ${count} files.`);
console.log('👉 Restart your server to see changes:');
console.log('   [Ctrl+C] to stop, then: npm run dev');
console.log('-----------------------------------------');