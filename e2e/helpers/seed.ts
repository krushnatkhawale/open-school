import { expect, type APIRequestContext } from '@playwright/test';

export const EXTRACTION_RULE_KEYS = [
  'classification',
  'imageDescription',
  'date',
  'scheduledEvents',
  'tags',
];

export async function seed(request: APIRequestContext): Promise<void> {
  const response = await request.post('/api/test/seed');
  expect(response.ok(), 'the e2e seed endpoint should succeed').toBeTruthy();
  expect(await response.text()).toContain('Seeded');
}

export async function resetExtractionRules(request: APIRequestContext): Promise<void> {
  for (const key of EXTRACTION_RULE_KEYS) {
    const response = await request.post(`/extraction-rules/${key}/reset`);
    expect(response.ok(), `resetting rule "${key}" should succeed`).toBeTruthy();
  }
}
