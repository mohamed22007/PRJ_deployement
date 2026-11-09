import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-plateau',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './plateau.html',
  styleUrl: './plateau.css',
})
export class PlateauComponent {
  @Input() plateau: number[] = [];
  @Input() carres: number[] = [];
  @Input() taille: number = 7;
  @Input() piececlick: number | null = null;
  @Input() pendingCapture: boolean = false;
  @Input() mustCaptureIndices: number[] = [];
  @Input() validMoveTargets: number[] = [];
  @Input() lastMovedIdx: number | null = null;
  @Input() readonly: boolean = false;
  @Input() flipped: boolean = false;

  @Output() pieceClicked = new EventEmitter<number>();

  onClick(idx: number) {
    if (!this.readonly) this.pieceClicked.emit(idx);
  }

  isStrongNode(index: number): boolean {
    const r = Math.floor(index / this.taille);
    const c = index % this.taille;
    return r % 2 === c % 2;
  }

  estbrown(index: number): boolean {
    const row = Math.floor(index / (this.taille - 1));
    const col = index % (this.taille - 1);
    return (row + col) % 2 !== 0;
  }

  getDiagonal(index: number): string {
    const row = Math.floor(index / (this.taille - 1));
    const col = index % (this.taille - 1);
    if ((Math.floor(row / 2) + Math.floor(col / 2)) % 2 === 0) {
      return row % 2 !== col % 2 ? 'diag-desc' : 'diag-asc';
    }
    return row % 2 === col % 2 ? 'diag-asc' : 'diag-desc';
  }

  getPieceX(index: number): number {
    return (index % this.taille) * 80 - 25;
  }

  getPieceY(index: number): number {
    return Math.floor(index / this.taille) * 80 - 25;
  }
}
