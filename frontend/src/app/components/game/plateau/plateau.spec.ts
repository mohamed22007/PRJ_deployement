import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PlateauComponent } from './plateau';

describe('PlateauComponent', () => {
  let fixture: ComponentFixture<PlateauComponent>;
  let component: PlateauComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [PlateauComponent] }).compileComponents();
    fixture = TestBed.createComponent(PlateauComponent);
    component = fixture.componentInstance;
  });

  it('isStrongNode : vrai quand ligne et colonne ont la même parité', () => {
    component.taille = 9;
    expect(component.isStrongNode(0)).toBe(true);    // (0,0)
    expect(component.isStrongNode(1)).toBe(false);   // (0,1)
    expect(component.isStrongNode(10)).toBe(true);   // (1,1)
    expect(component.isStrongNode(9)).toBe(false);   // (1,0)
  });

  it('getPieceX / getPieceY : position en pixels (80 px par case, décalage -25)', () => {
    component.taille = 9;
    expect(component.getPieceX(0)).toBe(-25);
    expect(component.getPieceY(0)).toBe(-25);
    expect(component.getPieceX(10)).toBe(55);   // colonne 1
    expect(component.getPieceY(10)).toBe(55);   // ligne 1
  });

  it('onClick : émet l\'index cliqué', () => {
    const emis: number[] = [];
    component.pieceClicked.subscribe((i) => emis.push(i));

    component.onClick(12);

    expect(emis).toEqual([12]);
  });

  it('onClick : n\'émet rien en mode lecture seule (spectateur / replay)', () => {
    const emis: number[] = [];
    component.pieceClicked.subscribe((i) => emis.push(i));
    component.readonly = true;

    component.onClick(12);

    expect(emis).toEqual([]);
  });

  it('affiche une pièce noire, une blanche et un mullah dans le DOM', () => {
    fixture.componentRef.setInput('taille', 3);
    fixture.componentRef.setInput('plateau', [1, 0, 2, 0, 0, 0, 3, 0, 0]);
    fixture.componentRef.setInput('carres', [0, 1, 2, 3]);
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.intersection').length).toBe(9);
    expect(el.querySelectorAll('.piece').length).toBe(3);
    expect(el.querySelectorAll('.piece.noire').length).toBe(2);   // pion noir + mullah noir
    expect(el.querySelectorAll('.piece.jaune').length).toBe(1);
    expect(el.querySelectorAll('.piece.mullah').length).toBe(1);
  });

  it('un clic dans le DOM sur une intersection émet son index', () => {
    fixture.componentRef.setInput('taille', 3);
    fixture.componentRef.setInput('plateau', [0, 0, 0, 0, 0, 0, 0, 0, 0]);
    fixture.detectChanges();
    const emis: number[] = [];
    component.pieceClicked.subscribe((i) => emis.push(i));

    const cases = fixture.nativeElement.querySelectorAll('.intersection');
    (cases[4] as HTMLElement).click();

    expect(emis).toEqual([4]);
  });
});
