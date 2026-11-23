import { afterEach, describe, expect, it } from 'vitest';
import '../src/mon-compteur.js';

async function mount() {
  const element = document.createElement('mon-compteur');
  document.body.append(element);
  await element.updateComplete;
  return element;
}

function button(element) {
  return element.shadowRoot.querySelector('wa-button');
}

describe('mon-compteur', () => {
  afterEach(() => {
    document.body.replaceChildren();
  });

  it('starts at zero', async () => {
    const element = await mount();

    expect(element.count).toBe(0);
    expect(button(element).textContent.trim()).toBe('Cliqué 0 fois');
  });

  it('counts clicks', async () => {
    const element = await mount();

    button(element).click();
    button(element).click();
    await element.updateComplete;

    expect(element.count).toBe(2);
    expect(button(element).textContent.trim()).toBe('Cliqué 2 fois');
  });

  it('renders the count property', async () => {
    const element = await mount();

    element.count = 41;
    await element.updateComplete;

    expect(button(element).textContent.trim()).toBe('Cliqué 41 fois');
  });
});
