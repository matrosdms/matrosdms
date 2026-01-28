import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

// --- CONFIGURATION ---
const OUTPUT_FILE = 'frontend-source.xml';

// Folders to strictly ignore
const IGNORED_DIRS = [
    'node_modules',
    '.git',
    '.idea',
    '.vscode',
    'dist',
    'build',
    'coverage',
    'public',
    'playwright-report',
    'target',
    'test-results',
    'tests'
	
	// Optional: usually contains static assets/images
];

// Files to strictly ignore
const IGNORED_FILES = [
    'package-lock.json',
    'yarn.lock',
    'pnpm-lock.yaml',
    '.DS_Store',
    'getAll.js', // Don't include self
    'apply_patch.js',
    'patch.xml',
    'app.xml',
    'init.xml',
    'update.xml'
];

// Allowed extensions (Source Code)
// If you have Java backend files in the same folder, add .java here
const ALLOWED_EXTS = [
    '.vue', '.js', '.ts', '.jsx', '.tsx', 
    '.html', '.css', '.scss', '.sass', '.less', 
    '.json', '.xml', '.sql', '.md', '.env', '.gitignore'
];

// --- SETUP ---
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

let fileCount = 0;
let outputBuffer = '<project>\n';

// --- LOGIC ---

function shouldInclude(filePath, isDir) {
    const name = path.basename(filePath);

    if (isDir) {
        return !IGNORED_DIRS.includes(name);
    }

    if (IGNORED_FILES.includes(name)) return false;
    
    // Check extension
    const ext = path.extname(name).toLowerCase();
    // Special case: include specific dotfiles like .gitignore
    if (name === '.gitignore' || name === '.env') return true;
    
    return ALLOWED_EXTS.includes(ext);
}

function scanDirectory(currentDir) {
    const items = fs.readdirSync(currentDir);

    for (const item of items) {
        const fullPath = path.join(currentDir, item);
        const stat = fs.statSync(fullPath);

        if (stat.isDirectory()) {
            if (shouldInclude(fullPath, true)) {
                scanDirectory(fullPath);
            }
        } else {
            if (shouldInclude(fullPath, false)) {
                addFileToXml(fullPath);
            }
        }
    }
}

function addFileToXml(fullPath) {
    try {
        // Read content
        const content = fs.readFileSync(fullPath, 'utf8');
        
        // Get relative path for the attribute (e.g., src/App.vue)
        const relPath = path.relative(__dirname, fullPath).replace(/\\/g, '/');

        // Escape CDATA closing tags if they exist in the code to prevent breaking XML
        const safeContent = content.replace(/]]>/g, ']]]]><![CDATA[>');

        outputBuffer += `<file path="${relPath}"><![CDATA[\n${safeContent}\n]]></file>\n`;
        
        console.log(`Packed: ${relPath}`);
        fileCount++;
    } catch (e) {
        console.error(`Error reading ${fullPath}:`, e.message);
    }
}

// --- EXECUTION ---
console.log('📦 Starting Project Pack...');
scanDirectory(__dirname);

outputBuffer += '</project>';

fs.writeFileSync(path.join(__dirname, OUTPUT_FILE), outputBuffer);

console.log('------------------------------------------------');
console.log(`✅ Done! Packed ${fileCount} files into ${OUTPUT_FILE}`);
console.log('------------------------------------------------');