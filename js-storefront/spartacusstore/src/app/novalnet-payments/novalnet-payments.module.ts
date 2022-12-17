import { NgModule } from "@angular/core";
import { CheckoutPaymentAdapter } from '@spartacus/checkout/base/core';
import { NovalnetCheckoutPaymentAdapter } from "./checkout.adapter";

@NgModule({
  providers: [
    {
      provide: CheckoutPaymentAdapter,
      useClass: NovalnetCheckoutPaymentAdapter
    }
  ]
})
export class NovalnetPaymentsModule {}
