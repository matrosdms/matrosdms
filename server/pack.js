const fs = require('fs');
const path = require('path');

// CONFIGURATION
const OUTPUT_FILE = 'project_context.xml';

// Directories to ignore (standard Spring Boot & System folders)
const IGNORE_DIRS = [
    '.git', 
    '.idea', 
    '.vscode', 
    '.mvn', 
	'data', 
    'target', 
    'build', 
    'bin', 
    'node_modules',
    'logs'
];

// Files to ignore
const IGNORE_FILES = [
    'mvnw', 
    'mvnw.cmd', 
    'gradlew', 
    'gradlew.bat', 
    '.DS_Store',
    'pack-project.js', // Don't pack this script itself
    OUTPUT_FILE        // Don't pack the output file
];

// Allowed file extensions (Add more if you have frontend code or specific configs)
const ALLOWED_EXTS = [
    '.java', 
    '.xml', 
    '.yml', 
    '.yaml', 
    '.properties', 
    '.sql', 
    '.md', 
    '.gradle', 
    '.groovy',
    '.jsp',
    '.html',
    '.css',
    '.js'
];

/**
 * Wraps content in CDATA to prevent XML parsing errors with code characters like < > &
 */
function wrapCDATA(content) {
    // Escape existing CDATA end sequences to avoid breaking the XML
    const safeContent = content.replace(/]]>/g, ']]]]><![CDATA[>');
    return `<![CDATA[\n${safeContent}\n]]>`;
}

function getAllFiles(dirPath, arrayOfFiles) {
    const files = fs.readdirSync(dirPath);

    arrayOfFiles = arrayOfFiles || [];

    files.forEach(function(file) {
        const fullPath = path.join(dirPath, file);
        
        if (fs.statSync(fullPath).isDirectory()) {
            if (!IGNORE_DIRS.includes(file)) {
                arrayOfFiles = getAllFiles(fullPath, arrayOfFiles);
            }
        } else {
            if (!IGNORE_FILES.includes(file) && ALLOWED_EXTS.includes(path.extname(file))) {
                arrayOfFiles.push(fullPath);
            }
        }
    });

    return arrayOfFiles;
}

function generateXML() {
    console.log(`\n📦 Scanning Spring Boot project in: ${process.cwd()}...`);
    
    const allFiles = getAllFiles(process.cwd());
    
    let xmlContent = '<?xml version="1.0" encoding="UTF-8"?>\n<project_codebase>\n';
    
    let fileCount = 0;

    allFiles.forEach(filePath => {
        try {
            const relativePath = path.relative(process.cwd(), filePath);
            const content = fs.readFileSync(filePath, 'utf8');
            
            xmlContent += `  <file path="${relativePath}">\n`;
            xmlContent += `    ${wrapCDATA(content)}\n`;
            xmlContent += `  </file>\n`;
            
            fileCount++;
            console.log(`   + Included: ${relativePath}`);
        } catch (err) {
            console.error(`   ! Error reading ${filePath}: ${err.message}`);
        }
    });

    xmlContent += '</project_codebase>';

    fs.writeFileSync(OUTPUT_FILE, xmlContent);
    
    console.log(`\n✅ Done! Packed ${fileCount} files into '${OUTPUT_FILE}'`);
    console.log(`⚠️  REMINDER: Check '${OUTPUT_FILE}' for sensitive API keys before sharing.`);
}

generateXML();