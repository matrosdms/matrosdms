const fs = require('fs');
const path = require('path');

// 1. Read the init.xml file
const xmlPath = path.join(__dirname, 'init.xml');

if (!fs.existsSync(xmlPath)) {
    console.error('❌ Error: init.xml not found in this directory.');
    process.exit(1);
}

const xmlContent = fs.readFileSync(xmlPath, 'utf8');

// 2. Simple Regex Parser to extract <file path="..."> content </file>
// This regex looks for: <file path="MATCH1"><![CDATA[MATCH2]]></file>
const fileRegex = /<file path="(.*?)"><!\[CDATA\[([\s\S]*?)\]\]><\/file>/g;

let match;
let count = 0;

console.log('🚀 Starting Installation...');

while ((match = fileRegex.exec(xmlContent)) !== null) {
    const filePath = match[1];
    const fileContent = match[2].trim(); // Trim CDATA wrapper whitespace

    // Ensure directory exists
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }

    // Write file
    fs.writeFileSync(filePath, fileContent);
    console.log(`   📄 Created: ${filePath}`);
    count++;
}

console.log('-----------------------------------------');
console.log(`✅ Success! Generated ${count} files.`);
console.log('-----------------------------------------');
console.log('👉 Now run the following commands:');
console.log('');
console.log('   npm install');
console.log('   npm run dev');
console.log('');