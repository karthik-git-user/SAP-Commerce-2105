import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfigModule, CmsConfig, I18nModule, UrlModule } from '@spartacus/core';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, } from '@angular/core';
import { Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { GlobalMessageType, } from '@spartacus/core';
import { AddressBookModule, AddressFormModule, CardModule, CartSharedModule, FormErrorsModule, IconModule, ICON_TYPE, OrderOverviewModule, PromotionsModule, PwaModule, SpinnerModule, SuggestedAddressDialogComponent, } from '@spartacus/storefront';
import { BehaviorSubject, combineLatest } from 'rxjs';
import { map, switchMap, tap } from 'rxjs/operators';
import * as i0 from "@angular/core";
import * as i1 from "@spartacus/checkout/root";
import * as i2 from "@spartacus/core";
import * as i3 from "@angular/forms";
import * as i4 from "@spartacus/storefront";
import * as i5 from "@ng-select/ng-select";
import * as i6 from "@angular/common";
import { CheckoutService, CheckoutDeliveryService, CheckoutPaymentService } from '@spartacus/checkout/core';
import { CheckoutStepService, OrderConfirmationModule, PaymentFormComponent, PaymentMethodComponent, ShippingAddressModule, PaymentFormModule} from '@spartacus/checkout/components';
import { NgSelectModule } from '@ng-select/ng-select';
import { ScriptService } from './payment-details-form/services/script.service';
import { PaymentDetailsFormComponent } from './payment-details-form/payment-details-form.component';

@NgModule({
  declarations: [
    PaymentDetailsFormComponent
  ],
  imports: [
    CommonModule,
    ConfigModule.withConfig({
      cmsComponents : {
        ProductSummaryComponent : {
          component : PaymentDetailsFormComponent
        }
      }
    } as CmsConfig),
    i3.FormsModule,
  I18nModule,
  UrlModule,
  CardModule,
  IconModule,
  PromotionsModule,
  CartSharedModule,
  FormErrorsModule,
  RouterModule,
  PaymentFormModule,
  NgSelectModule,
  ShippingAddressModule,
  AddressFormModule,
  AddressBookModule,
  i3.ReactiveFormsModule,
  OrderOverviewModule,
  SpinnerModule,
  PwaModule,
  OrderConfirmationModule
  ],
  providers: [
    ScriptService, CheckoutDeliveryService
  ]
})
export class NovalnetPaymentsModule {}
