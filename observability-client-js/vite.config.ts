
import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';
import { resolve } from 'path';

export default defineConfig({
    build: {
        lib: {
            entry: resolve(__dirname, 'src/index.ts'),
            name: 'ObservabilityClient',
            fileName: 'observability-client',
            formats: ['es', 'cjs', 'umd']
        },
        rollupOptions: {
            // make sure to externalize deps that shouldn't be bundled
            // into your library
            external: [],
            output: {
                // Provide global variables to use in the UMD build
                // for externalized deps
                globals: {}
            }
        }
    },
    plugins: [dts({
        insertTypesEntry: true,
    })],
});
