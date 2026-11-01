import { test, expect } from '@playwright/test';
import { seed } from './helpers/seed';

const ONBOARDING_TEXT = 'Welcome! What is the name of the school you represent?';

test.describe('JSON API — contract used by the browser UI', () => {

  test.beforeEach(async ({ request }) => {
    await seed(request);
  });

  test('lists only visible records and never exposes image bytes', async ({ request }) => {
    const response = await request.get('/api/classifications');
    expect(response.status()).toBe(200);

    const records = await response.json();
    expect(records).toHaveLength(4);
    expect(records.map((r: any) => r.originalText)).not.toContain(ONBOARDING_TEXT);
    for (const record of records) {
      expect(record.imageData).toBeUndefined();
      expect(record.feedHidden).toBe(false);
      expect(record.communicationType).toBeTruthy();
      expect(record.eventDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    }
  });

  test('filters by communication type and by chat id', async ({ request }) => {
    const byType = await request.get('/api/classifications?type=REMINDER');
    expect(byType.status()).toBe(200);
    const reminders = await byType.json();
    expect(reminders).toHaveLength(1);
    expect(reminders[0].communicationType).toBe('REMINDER');

    const byChat = await request.get('/api/classifications?chatId=67890');
    expect(byChat.status()).toBe(200);
    const chatRecords = await byChat.json();
    expect(chatRecords).toHaveLength(2);
    expect(chatRecords.every((r: any) => r.chatId === 67890)).toBe(true);
  });

  test('returns a single record and 404 for unknown ids', async ({ request }) => {
    const list = await (await request.get('/api/classifications')).json();
    const id = list[0].id;

    const found = await request.get(`/api/classifications/${id}`);
    expect(found.status()).toBe(200);
    expect((await found.json()).id).toBe(id);

    const missing = await request.get('/api/classifications/00000000-0000-0000-0000-000000000000');
    expect(missing.status()).toBe(404);
  });

  test('rejects invalid edits with field errors and accepts a valid edit', async ({ request }) => {
    const list = await (await request.get('/api/classifications')).json();
    const id = list[0].id;

    const missingType = await request.put(`/api/classifications/${id}`, {
      data: { eventDate: '2026-08-10', originalText: 'x' },
    });
    expect(missingType.status()).toBe(400);
    const missingTypeBody = await missingType.json();
    expect(missingTypeBody.detail).toBe('Request validation failed');
    expect(missingTypeBody.fieldErrors[0].field).toBe('communicationType');

    const missingDate = await request.put(`/api/classifications/${id}`, {
      data: { communicationType: 'REMINDER', originalText: 'x' },
    });
    expect(missingDate.status()).toBe(400);
    expect((await missingDate.json()).fieldErrors[0].field).toBe('eventDate');

    const malformed = await request.put(`/api/classifications/${id}`, {
      data: { communicationType: 'NOT_A_TYPE', eventDate: '2026-08-10' },
    });
    expect(malformed.status()).toBe(400);
    expect((await malformed.json()).detail).toBe('Request body is missing or malformed');

    const unknown = await request.put('/api/classifications/00000000-0000-0000-0000-000000000000', {
      data: { communicationType: 'REMINDER', eventDate: '2026-08-10' },
    });
    expect(unknown.status()).toBe(404);

    const valid = await request.put(`/api/classifications/${id}`, {
      data: {
        communicationType: 'EVENT',
        eventDate: '2026-08-10',
        originalText: '  Updated through the API  ',
        tags: ' maths , english , maths ',
      },
    });
    expect(valid.status()).toBe(200);
    const updated = await valid.json();
    expect(updated.communicationType).toBe('EVENT');
    expect(updated.eventDate).toBe('2026-08-10');
    expect(updated.originalText).toBe('Updated through the API');
    expect(updated.tags).toBe('maths,english');
  });

  test('serves stored images with private caching and 404s for text-only records', async ({ request }) => {
    const records = await (await request.get('/api/classifications')).json();
    const withImage = records.find((r: any) => r.imageContentType != null);
    const withoutImage = records.find((r: any) => r.imageContentType == null);
    expect(withImage).toBeTruthy();
    expect(withoutImage).toBeTruthy();

    const image = await request.get(`/api/classifications/${withImage.id}/image`);
    expect(image.status()).toBe(200);
    expect(image.headers()['content-type']).toContain('image/png');
    expect(image.headers()['cache-control']).toBe('private, max-age=31536000');
    expect((await image.body()).length).toBeGreaterThan(0);

    const noImage = await request.get(`/api/classifications/${withoutImage.id}/image`);
    expect(noImage.status()).toBe(404);
  });

  test('health endpoint reports UP', async ({ request }) => {
    const response = await request.get('/actuator/health');
    expect(response.status()).toBe(200);
    expect((await response.json()).status).toBe('UP');
  });
});
