import globals from "globals";
import tseslint from "typescript-eslint";
import pluginVue from "eslint-plugin-vue";

export default tseslint.config(
  { ignores: ["dist", "node_modules", "target", "**/*.d.ts", "*.js", "*.mjs"] },
  {
    files: ["**/*.{js,mjs,cjs,ts,mts,cts,vue}"],
    languageOptions: {
      globals: globals.browser,
      ecmaVersion: 2022,
      sourceType: "module",
    },
  },
  ...tseslint.configs.recommended,
  ...pluginVue.configs["flat/recommended"],
  {
    files: ["**/*.vue"],
    languageOptions: {
      parserOptions: {
        parser: tseslint.parser,
        extraFileExtensions: [".vue"],
        sourceType: "module",
      },
    },
  },
  {
    rules: {
      // Disable stylistic rules - let formatter handle these
      "@typescript-eslint/no-explicit-any": "warn",
      "@typescript-eslint/no-unused-vars": ["warn", { argsIgnorePattern: "^_" }],
      "@typescript-eslint/no-require-imports": "off",
      "vue/multi-word-component-names": "off",
      "vue/html-indent": "off",
      "vue/max-attributes-per-line": "off",
      "vue/html-self-closing": "off",
      "vue/singleline-html-element-content-newline": "off",
      "vue/multiline-html-element-content-newline": "off",
      "vue/attributes-order": "off",
      "vue/attribute-hyphenation": "off",
    },
  }
);
