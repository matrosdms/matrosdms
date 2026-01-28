import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.join(__dirname, '../');

// List of files to permanently delete
const filesToDelete = [
    'src/stores/category.ts',
    'src/lib/queryKeys.ts' // Replaced by src/lib/queries.ts
];

console.log('🧹 Cleaning up legacy files...');

filesToDelete.forEach(file => {
    const fullPath = path.join(projectRoot, file);
    if (fs.existsSync(fullPath)) {
        try {
            fs.unlinkSync(fullPath);
            console.log(`   ✅ Deleted: ${file}`);
        } catch (e) {
            console.error(`   ❌ Failed to delete ${file}:`, e.message);
        }
    } else {
        console.log(`   ℹ️  Skipped (not found): ${file}`);
    }
});

console.log('------------------------------------------------');
console.log('✨ Cleanup complete.');
console.log('------------------------------------------------');