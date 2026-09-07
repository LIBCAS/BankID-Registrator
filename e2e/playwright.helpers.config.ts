import config from './playwright.config';

// Isolated browser fixtures: no Bank iD, Aleph, or Comgate requests.
export default {
  ...config,
  testDir: './helper-tests',
  outputDir: './helper-test-results',
  reporter: 'list' as const,
  retries: 0,
};
