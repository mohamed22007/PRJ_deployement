import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-regles',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './regles.html',
  styleUrl: './regles.css',
})
export class Regles {
  active = 0;

  // Plateau 5×5 : espacement 70px, marge 10px → viewBox "0 0 300 300"
  readonly S = 70;
  readonly M = 10;
  readonly N = 5;

  nx(c: number) { return this.M + c * this.S; }
  ny(r: number) { return this.M + r * this.S; }
  isStrong(r: number, c: number) { return r % 2 === c % 2; }

  // Lignes horizontales
  readonly hLines = Array.from({ length: this.N }, (_, r) =>
    Array.from({ length: this.N - 1 }, (__, c) => ({
      x1: this.nx(c), y1: this.ny(r), x2: this.nx(c + 1), y2: this.ny(r)
    }))
  ).flat();

  // Lignes verticales
  readonly vLines = Array.from({ length: this.N }, (_, c) =>
    Array.from({ length: this.N - 1 }, (__, r) => ({
      x1: this.nx(c), y1: this.ny(r), x2: this.nx(c), y2: this.ny(r + 1)
    }))
  ).flat();

  // Lignes diagonales (seulement depuis les nœuds forts, vers le bas)
  readonly dLines = (() => {
    const lines: {x1:number,y1:number,x2:number,y2:number}[] = [];
    for (let r = 0; r < this.N - 1; r++) {
      for (let c = 0; c < this.N; c++) {
        if (this.isStrong(r, c)) {
          if (c > 0)          lines.push({ x1: this.nx(c), y1: this.ny(r), x2: this.nx(c-1), y2: this.ny(r+1) });
          if (c < this.N - 1) lines.push({ x1: this.nx(c), y1: this.ny(r), x2: this.nx(c+1), y2: this.ny(r+1) });
        }
      }
    }
    return lines;
  })();

  // Tous les nœuds (pour afficher les intersections)
  readonly allNodes = Array.from({ length: this.N }, (_, r) =>
    Array.from({ length: this.N }, (__, c) => ({ r, c, strong: this.isStrong(r, c) }))
  ).flat();

  readonly demos = [
    {
      label: 'Déplacer',
      title: 'Déplacer une pièce',
      text: 'Un pion avance d\'une case libre le long d\'une ligne. Sur un nœud fort ⊕ (croix avec diagonales), il peut aussi se déplacer en diagonale — 8 directions au lieu de 4.',
    },
    {
      label: 'Capturer',
      title: 'Capturer — obligatoire',
      text: 'Sautez par-dessus un pion adverse vers une case vide derrière lui. La capture est OBLIGATOIRE si elle est disponible — impossible de faire un simple déplacement.',
    },
    {
      label: 'Enchaîner',
      title: 'Séquence de captures',
      text: 'Après un saut, si une autre capture est encore possible depuis la nouvelle position — vous DEVEZ continuer dans le même tour. Le tour se termine automatiquement quand plus aucun saut n\'est possible.',
    },
    {
      label: 'Le Moul',
      title: 'Le Moul — Dame',
      text: 'Un pion qui atteint la dernière rangée adverse est promu en Moul (♛). Le Moul glisse sur autant de cases qu\'il veut en ligne droite et peut capturer un pion adverse à distance en atterrissant derrière lui.',
    },
  ];
}
