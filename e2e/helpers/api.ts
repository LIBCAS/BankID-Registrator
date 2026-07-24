/**
 * Helper for interacting with the app's REST APIs during tests.
 * Uses Playwright's APIRequestContext (no browser needed).
 */
import { APIRequestContext } from '@playwright/test';
import { env } from './env';

/**
 * Generate a unique middle name via the Tester's Toolkit API and save it.
 * Returns the generated middle name string.
 */
export async function generateAndSetMiddleName(request: APIRequestContext): Promise<string> {
  // Generate a random middle name
  const genResponse = await request.get(`${env.contextPath}/api/test-settings/generate-middle-name`);
  if (!genResponse.ok()) {
    throw new Error(`Failed to generate middle name: ${genResponse.status()}`);
  }
  const { middleName } = await genResponse.json();

  // Save it
  const putResponse = await request.put(`${env.contextPath}/api/test-settings`, {
    params: { middleName },
  });
  if (!putResponse.ok()) {
    throw new Error(`Failed to set middle name: ${putResponse.status()}`);
  }

  return middleName;
}

/**
 * Set the middle name to a specific value.
 */
export async function setMiddleName(request: APIRequestContext, middleName: string): Promise<void> {
  const response = await request.put(`${env.contextPath}/api/test-settings`, {
    params: { middleName },
  });
  if (!response.ok()) {
    throw new Error(`Failed to set middle name: ${response.status()}`);
  }
}

/**
 * Set the force renewal flag.
 */
export async function setForceRenewal(request: APIRequestContext, enabled: boolean): Promise<void> {
  const response = await request.put(`${env.contextPath}/api/test-settings`, {
    params: { forceRenewal: String(enabled) },
  });
  if (!response.ok()) {
    throw new Error(`Failed to set forceRenewal: ${response.status()}`);
  }
}

/**
 * Get current test settings.
 */
export async function getTestSettings(request: APIRequestContext): Promise<{
  middleName: string;
  forceRenewal: boolean;
  updatedAt: string;
}> {
  const response = await request.get(`${env.contextPath}/api/test-settings`);
  if (!response.ok()) {
    throw new Error(`Failed to get test settings: ${response.status()}`);
  }
  return response.json();
}
