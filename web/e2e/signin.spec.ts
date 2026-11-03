import { expect, test, type Page } from '@playwright/test';

async function signIn(page: Page, username: string) {
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill('unischool');
  await page.getByRole('button', { name: 'Sign in' }).click();
}

test('wrong password shows the backend message', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Username').fill('naledi');
  await page.getByLabel('Password').fill('not-the-password');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page.getByRole('alert')).toHaveText("That username and password don't match.");
});

test('principal sees the dashboard and can switch to the school office', async ({ page }) => {
  await signIn(page, 'khumalo');
  await expect(page).toHaveURL(/\/principal$/);
  await expect(page.getByRole('heading', { name: 'Waiting for you', exact: false })).toBeVisible();

  await page.getByRole('group', { name: 'Switch role' }).getByRole('button', { name: 'School office' }).click();
  await expect(page).toHaveURL(/\/office$/);
  await expect(page.getByRole('button', { name: 'School office' })).toHaveAttribute('aria-pressed', 'true');
});

test('signed-out visitors are sent to sign in', async ({ page }) => {
  await page.goto('/principal/approvals');
  await expect(page).toHaveURL(/\/login$/);
});

test('sign out clears the session', async ({ page }) => {
  await signIn(page, 'naledi');
  await expect(page).toHaveURL(/\/parent$/);
  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto('/parent');
  await expect(page).toHaveURL(/\/login$/);
});
