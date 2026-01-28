import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const srcDir = path.join(__dirname, '../src');

// Recursive walker
function walk(dir, callback) {
    const files = fs.readdirSync(dir);
    files.forEach(file => {
        const filepath = path.join(dir, file);
        const stats = fs.statSync(filepath);
        if (stats.isDirectory()) {
            walk(filepath, callback);
        } else if (stats.isFile() && file.endsWith('.vue')) {
            callback(filepath);
        }
    });
}

console.log('🔄 Starting UI Migration (App* -> Base*)...');

let count = 0;

walk(srcDir, (filepath) => {
    let content = fs.readFileSync(filepath, 'utf8');
    let changed = false;

    // 1. Replace Imports
    if (content.includes('import AppButton')) {
        content = content.replace(/import AppButton from ['"]@\/components\/ui\/AppButton\.vue['"]/g, "import BaseButton from '@/components/ui/BaseButton.vue'");
        changed = true;
    }
    if (content.includes('import AppInput')) {
        content = content.replace(/import AppInput from ['"]@\/components\/ui\/AppInput\.vue['"]/g, "import BaseInput from '@/components/ui/BaseInput.vue'");
        changed = true;
    }

    // 2. Replace Tags
    if (content.includes('<AppButton')) {
        content = content.replace(/<AppButton/g, '<BaseButton').replace(/<\/AppButton>/g, '</BaseButton>');
        changed = true;
    }
    if (content.includes('<AppInput')) {
        content = content.replace(/<AppInput/g, '<BaseInput').replace(/<\/AppInput>/g, '</BaseInput>');
        changed = true;
    }

    // 3. Map Props (AppButton variant="primary" -> BaseButton variant="default")
    // Note: BaseButton uses 'default' for primary color to match Shadcn convention
    if (content.includes('variant="primary"')) {
        content = content.replace(/variant="primary"/g, 'variant="default"');
        changed = true;
    }

    if (changed) {
        fs.writeFileSync(filepath, content);
        console.log(`   📝 Updated: ${path.relative(srcDir, filepath)}`);
        count++;
    }
});

console.log(`✅ Migration complete. Updated ${count} files.`);
console.log('👉 Tip: Manually check complex usages of AppInput props.');