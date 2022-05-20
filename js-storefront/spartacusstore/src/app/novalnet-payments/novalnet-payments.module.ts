import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentDetailsFormComponent } from './payment-details-form/payment-details-form.component';
import { CmsConfig, ConfigModule } from '@spartacus/core';



@NgModule({
  declarations: [
    PaymentDetailsFormComponent
  ],
  imports: [
    CommonModule,
    ConfigModule.withConfig({
      cmsComponents : {
        CheckoutPaymentDetails : {
          component : PaymentDetailsFormComponent
        }
      }
    } as CmsConfig),
  ]
})
export class NovalnetPaymentsModule { }
