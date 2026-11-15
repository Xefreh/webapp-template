import { LitElement, html } from 'lit';

class MonCompteur extends LitElement {
  static properties = { count: { type: Number } };

  constructor() {
    super();
    this.count = 0;
  }

  render() {
    return html`
      <wa-button variant="brand" @click=${() => this.count++}>
        Cliqué ${this.count} fois
      </wa-button>`;
  }
}

customElements.define('mon-compteur', MonCompteur);