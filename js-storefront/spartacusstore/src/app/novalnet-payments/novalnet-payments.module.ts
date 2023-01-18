import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfigModule, CmsConfig, provideConfig, OccFieldsService, OccEndpointsService, BaseOccModule, I18nModule, UrlModule} from '@spartacus/core';
import { HttpClientModule } from '@angular/common/http';

import { PaymentDetailsFormComponent } from './payment-details-form/payment-details-form.component';
import { CheckoutStepType } from '@spartacus/checkout/base/root/model/checkout-step.model';
import { CheckoutOccModule, OccCheckoutAdapter } from '@spartacus/checkout/base/occ';
import { SpartacusModule } from '../spartacus/spartacus.module';
import { CartNotEmptyGuard, CheckoutAuthGuard, CheckoutDeliveryAddressModule, CheckoutOrderSummaryComponent, CheckoutPlaceOrderComponent, CheckoutReviewSubmitComponent, CheckoutStepsSetGuard } from '@spartacus/checkout/base/components';
import { AddressBookModule, AddressFormModule, CardModule, FormErrorsModule, IconModule, PromotionsModule, PwaModule, SpinnerModule } from '@spartacus/storefront';
import { RouterModule, UrlTree } from '@angular/router';
import { NgSelectModule } from '@ng-select/ng-select';
import { CartSharedModule } from '@spartacus/cart/base/components';
import { OrderOverviewModule } from '@spartacus/order/components';
import { ReactiveFormsModule } from '@angular/forms';
import { Observable, of } from 'rxjs';

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
        },
        CheckoutReviewOrder: {
          component : CheckoutReviewSubmitComponent,
          guards : []
        },
        CkeckoutPlaceOrder: {
          component : CheckoutPlaceOrderComponent,
          guards: []
        }
      }
    } as CmsConfig),
    HttpClientModule,
    CheckoutDeliveryAddressModule,
    CheckoutOccModule,
    BaseOccModule,
    CardModule,
    I18nModule,
    UrlModule,
    IconModule,
    PromotionsModule,
    CartSharedModule,
    FormErrorsModule,
    RouterModule,
    NgSelectModule,
    AddressFormModule,
    AddressBookModule,
    OrderOverviewModule,
    SpinnerModule,
    PwaModule,
    ReactiveFormsModule 
  ],
  providers: [

    provideConfig({
      routing: {
        routes: {
          checkout: {
            paths: ['checkout'],
          },
          checkoutDeliveryAddress: {
            paths: ['checkout/delivery-address']
          },
          checkoutDeliveryMode: {
            paths: ['checkout/delivery-mode']
          },
          checkoutNovalnetPayment: {
             paths: ['checkout/novalnet-payment']
          },
          checkoutPaymentDetails: {
            paths: ['checkout/payment-details']
          },
          checkoutReviewOrder: {
            paths: ['checkout/review-order']
          }
        },
      },
      checkout: {
        steps: [
          {
            id: 'deliveryAddress',
            name: 'checkoutProgress.deliveryAddress',
            routeName: 'checkoutDeliveryAddress',
            type: [CheckoutStepType.DELIVERY_ADDRESS],
          },
          {
            id: 'deliveryMode',
            name: 'checkoutProgress.deliveryMode',
            routeName: 'checkoutDeliveryMode',
            type: [CheckoutStepType.DELIVERY_MODE],
          },
          {
            id: 'paymentDetails',
            name: 'checkoutProgress.paymentDetails',
            routeName: 'checkoutPaymentDetails',
            type: [CheckoutStepType.PAYMENT_DETAILS],
          },
          {
            id: 'reviewOrder',
            name: 'checkoutProgress.reviewOrder',
            routeName: 'checkoutReviewOrder',
            type: [CheckoutStepType.REVIEW_ORDER],
          },
        ],
      },
    }),
  ]
})
export class NovalnetPaymentsModule {


}
