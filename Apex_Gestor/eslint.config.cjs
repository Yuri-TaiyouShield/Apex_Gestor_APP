const js = require('@eslint/js');
const angular = require('angular-eslint');
const tseslint = require('typescript-eslint');

const commonGlobals = {
  Buffer: 'readonly',
  console: 'readonly',
  crypto: 'readonly',
  globalThis: 'readonly',
  localStorage: 'readonly',
  module: 'readonly',
  process: 'readonly',
  require: 'readonly',
  sessionStorage: 'readonly',
  window: 'readonly',
  __dirname: 'readonly',
  URL: 'readonly'
};

module.exports = tseslint.config(
  {
    ignores: [
      'android/**',
      'coverage/**',
      'dist/**',
      'node_modules/**',
      'out-tsc/**',
      'release/**',
      'release-check/**',
      '.angular/**',
      '.electron-package/**'
    ]
  },
  {
    files: ['**/*.ts'],
    extends: [
      ...tseslint.configs.recommended,
      ...angular.configs.tsRecommended
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@angular-eslint/component-class-suffix': 'off',
      '@typescript-eslint/no-empty-function': 'off'
    }
  },
  {
    files: ['**/*.html'],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility
    ]
  },
  {
    files: ['electron/**/*.cjs', 'scripts/**/*.cjs', 'eslint.config.cjs'],
    ...js.configs.recommended,
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: 'commonjs',
      globals: commonGlobals
    }
  }
);
