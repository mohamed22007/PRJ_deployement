import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Histoire } from './histoire';

describe('Histoire', () => {
  let component: Histoire;
  let fixture: ComponentFixture<Histoire>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Histoire],
    }).compileComponents();

    fixture = TestBed.createComponent(Histoire);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
