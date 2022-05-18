import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddCustomPaymentsComponent } from './add-custom-payments.component';

describe('AddCustomPaymentsComponent', () => {
  let component: AddCustomPaymentsComponent;
  let fixture: ComponentFixture<AddCustomPaymentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ AddCustomPaymentsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AddCustomPaymentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
