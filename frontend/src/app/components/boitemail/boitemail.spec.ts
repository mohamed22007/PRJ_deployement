import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Boitemail } from './boitemail';

describe('Boitemail', () => {
  let component: Boitemail;
  let fixture: ComponentFixture<Boitemail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Boitemail],
    }).compileComponents();

    fixture = TestBed.createComponent(Boitemail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
