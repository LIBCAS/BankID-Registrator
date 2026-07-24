/**
 * Generate random lowercase a-z characters.
 */
export function randomSuffix(length = 7): string {
  const chars = 'abcdefghijklmnopqrstuvwxyz';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

/**
 * Generate a random alphanumeric string (lowercase a-z + digits 0-9).
 */
export function randomAlphanumeric(length = 8): string {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}

/**
 * Generate a random email address with the given domain.
 */
export function randomEmail(domain: string, length = 8): string {
  return `${randomAlphanumeric(length)}@${domain}`;
}
