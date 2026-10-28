import { Component, computed, input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { inject } from '@angular/core';
import qrcode from 'qrcode-generator';

// Desenha o QR Code do Pix a partir do código "copia e cola" (gerado no próprio navegador, sem internet).
@Component({
  selector: 'app-pix-qr',
  standalone: true,
  template: `<div class="[&>svg]:h-auto [&>svg]:w-full" [innerHTML]="svg()"></div>`,
})
export class PixQrComponent {
  private sanitizer = inject(DomSanitizer);
  codigo = input.required<string>();

  svg = computed<SafeHtml>(() => {
    const qr = qrcode(0, 'M');
    qr.addData(this.codigo());
    qr.make();
    // O SVG é montado pela biblioteca a partir de texto nosso (o BR Code), não de conteúdo do usuário
    return this.sanitizer.bypassSecurityTrustHtml(qr.createSvgTag({ cellSize: 6, margin: 2, scalable: true }));
  });
}
