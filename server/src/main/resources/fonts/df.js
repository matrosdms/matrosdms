const https = require('https');
const fs = require('fs');
const path = require('path');

// 1. Define the fonts you want to download
//    (These are stable, raw links from public open-source repos)
const fontsToDownload = [
    {
        name: 'Roboto-Regular.ttf',
        url: 'https://github.com/googlefonts/roboto/raw/main/src/hinted/Roboto-Regular.ttf'
    },
    {
        name: 'OpenSans-Regular.ttf',
        url: 'https://github.com/googlefonts/opensans/raw/main/fonts/ttf/OpenSans-Regular.ttf'
    }
];

// 2. Define the output directory (same directory as this script)
const OUTPUT_DIR = __dirname;

// Ensure the directory exists (should already exist since this script lives there)
if (!fs.existsSync(OUTPUT_DIR)) {
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
    console.log(`Created directory: ${OUTPUT_DIR}`);
}

/**
 * Downloads a file from a URL to a local destination.
 * @param {string} url - Direct download URL
 * @param {string} dest - Local file path
 */
const downloadFont = (url, dest) => {
    return new Promise((resolve, reject) => {
        const file = fs.createWriteStream(dest);

        const request = https.get(url, (response) => {
            // Check for valid status code (200)
            if (response.statusCode === 200) {
                response.pipe(file);
            } else if (response.statusCode === 302 || response.statusCode === 301) {
                // Handle redirects (GitHub raw links often redirect)
                console.log(`-> Redirecting to: ${response.headers.location}`);
                downloadFont(response.headers.location, dest)
                    .then(resolve)
                    .catch(reject);
                return;
            } else {
                file.close();
                fs.unlink(dest, () => {}); // Delete partial file
                reject(`Server responded with ${response.statusCode}: ${response.statusMessage}`);
                return;
            }

            file.on('finish', () => {
                file.close(() => {
                    console.log(`✓ Downloaded: ${path.basename(dest)}`);
                    resolve();
                });
            });
        });

        request.on('error', (err) => {
            file.close();
            fs.unlink(dest, () => {}); // Delete partial file
            reject(err.message);
        });

        file.on('error', (err) => {
            file.close();
            fs.unlink(dest, () => {}); // Delete partial file
            reject(err.message);
        });
    });
};

// 3. Execute the downloads
(async () => {
    console.log('Starting font downloads...\n');

    for (const font of fontsToDownload) {
        const destPath = path.join(OUTPUT_DIR, font.name);
        try {
            await downloadFont(font.url, destPath);
        } catch (err) {
            console.error(`X Failed to download ${font.name}: ${err}`);
        }
    }

    console.log('\nAll operations completed.');
})();