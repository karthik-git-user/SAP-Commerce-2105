import {Component, EventEmitter, OnInit} from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Address, ConverterService, Country, InterceptorUtil, OccEndpointsService, Region, RoutingService, StateUtils, UserAddressService, UserPaymentService, USE_CLIENT_TOKEN } from '@spartacus/core';
import { Occ } from '@spartacus/core';
import { OccCheckoutAdapter } from '@spartacus/checkout/base/occ';
import { CheckoutAdapter } from '@spartacus/checkout/base/core';
import {  CheckoutDeliveryAddressFacade, CheckoutPaymentFacade, CheckoutState } from '@spartacus/checkout/base/root';
import { BehaviorSubject, combineLatest, Observable } from 'rxjs';
import { map, filter, tap, shareReplay, switchMap, skipWhile, debounceTime, take, withLatestFrom, distinctUntilChanged, takeWhile } from 'rxjs/operators';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import * as i0 from "@angular/core";
import { CheckoutPaymentFormComponent, CheckoutPaymentMethodComponent, CheckoutStepService } from '@spartacus/checkout/base/components';
import { ActivatedRoute } from '@angular/router';


import * as i1 from "@spartacus/core";
import * as i2 from "@spartacus/checkout/base/root";
import * as i3 from "@angular/router";
import * as i4 from "@spartacus/cart/base/root";
import * as i5 from "@spartacus/checkout/base/components";

declare var data: any;
declare var address: any;
declare var result: any;
declare var country: any;

@Component({
  selector: 'app-payment-details-form',
  templateUrl: './payment-details-form.component.html',
  styleUrls: ['./payment-details-form.component.scss']
})
export class PaymentDetailsFormComponent implements OnInit{



  occEndpoint: OccEndpointsService;
  address: any;
  http: HttpClient;
  showSameAsDeliveryAddressCheckbox$: Observable<boolean>;
  protected checkoutStepService: CheckoutStepService;
  userPaymentService: UserPaymentService;
  // deliveryAddress$: any;
  checkoutDeliveryAddressFacade: CheckoutDeliveryAddressFacade;
  sameAsDeliveryAddress: boolean;
  countries$: Observable<Country[]>;
  deliveryAddress$: Observable<any>;
  selectedCountry$: BehaviorSubject<string>;
billingAddress: any;
regions$: Observable<Region[]>;
billingAddressForm: FormGroup;
paymentMethodsCount: number;
  routingService: RoutingService;
  protected busy$: BehaviorSubject<boolean>;

  static ɵcmp: i0.ɵɵComponentDeclaration<CheckoutPaymentMethodComponent, "cx-payment-method", never, { "setAsDefaultField": "setAsDefaultField"; "paymentMethodsCount": "paymentMethodsCount"; }, { "goBack": "goBack"; "closeForm": "closeForm"; "setPaymentDetails": "setPaymentDetails"; }, never, never>;


  constructor(occEndpoint:OccEndpointsService, http:HttpClient, userPaymentService: UserPaymentService, checkoutDeliveryAddressFacade: CheckoutDeliveryAddressFacade,private fb: FormBuilder,  private userAddressService: UserAddressService, checkoutStepService: CheckoutStepService, private activatedRoute: ActivatedRoute, routingService: RoutingService){
    this.routingService = routingService;
    // this.setPaymentDetails = new EventEmitter();
    this.busy$ = new BehaviorSubject(false);
    this.activatedRoute = activatedRoute;
      this.checkoutStepService = checkoutStepService;
      this.occEndpoint = occEndpoint;
      this.http = http;
      this.userPaymentService = userPaymentService;
      this.checkoutDeliveryAddressFacade = checkoutDeliveryAddressFacade;
      this.sameAsDeliveryAddress = true;
      this.userAddressService = userAddressService;
      this.paymentMethodsCount = 0;
      this.selectedCountry$ = new BehaviorSubject('');
      this.countries$ = this.userPaymentService.getAllBillingCountries().pipe(tap((countries) => {
        // If the store is empty fetch countries. This is also used when changing language.
        if (Object.keys(countries).length === 0) {
            this.userPaymentService.loadBillingCountries();
        }
    }));

    this.billingAddressForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      line1: ['', Validators.required],
      line2: [''],
      town: ['', Validators.required],
      region: this.fb.group({
          isocodeShort: [null, Validators.required],
      }),
      country: this.fb.group({
          isocode: [null, Validators.required],
      }),
      postalCode: ['', Validators.required],
  });

      this.deliveryAddress$ = this.checkoutDeliveryAddressFacade
            .getDeliveryAddressState()
            .pipe(filter((state) => !state.loading), map((state) => state.data));
        this.showSameAsDeliveryAddressCheckbox$ = combineLatest([
            this.countries$,
            this.deliveryAddress$,
        ]).pipe(map(([countries, address]) => {
            return ((address?.country &&
                !!countries.filter((country) => country.isocode === address.country?.isocode).length) ??
                false);
        }), tap((shouldShowCheckbox) => {
            this.sameAsDeliveryAddress = shouldShowCheckbox;
        }));

        this.regions$ = this.selectedCountry$.pipe(switchMap((country) => this.userAddressService.getRegions(country)), tap((regions) => {
          const regionControl = this.billingAddressForm.get('region.isocodeShort');
          if (regions.length > 0) {
              regionControl === null || regionControl === void 0 ? void 0 : regionControl.enable();
          }
          else {
              regionControl === null || regionControl === void 0 ? void 0 : regionControl.disable();
          }
      }));

      

    this.showSameAsDeliveryAddressCheckbox$ = combineLatest([
      this.countries$,
      this.deliveryAddress$,
  ]).pipe(map(([countries, address]) => {
      return ((address?.country &&
          !!countries.filter((country) => country.isocode === address.country?.isocode).length) ??
          false);
  }), tap((shouldShowCheckbox) => {
      this.sameAsDeliveryAddress = shouldShowCheckbox;
  }));

  // this.deliveryAddress$ = this.checkoutDeliveryAddressFacade.getDeliveryAddressState().pipe(filter((state) => !state.loading && !state.error), map((state) => state.data));

    

    

      
  }
  ngOnInit(): void {	
    let url = this.occEndpoint.getBaseUrl()+"/novalnet/config/details";
    this.http.get(url)
    .subscribe((response: any) => console.log(response));
  }

  toggleSameAsDeliveryAddress() {
    this.sameAsDeliveryAddress = !this.sameAsDeliveryAddress;
}

next() {
  this.checkoutStepService.next(this.activatedRoute);
}

back() {
  this.checkoutStepService.back(this.activatedRoute);
  return false;
}

close () {}

public countrySelected(country:any) {
  var _a;
  (_a = this.billingAddressForm.get('country.isocode')) === null || _a === void 0 ? void 0 : _a.setValue(country.isocode);
  this.selectedCountry$.next(country.isocode);
}

public getAddressCardContent(address: any) {
  var _a;
  let region = '';
  if (address.region && address.region.isocode) {
      region = address.region.isocode + ', ';
  }
  return {
      textBold: address.firstName + ' ' + address.lastName,
      text: [
          address.line1,
          address.line2,
          address.town + ', ' + region + ((_a = address.country) === null || _a === void 0 ? void 0 : _a.isocode),
          address.postalCode,
          address.phone,
      ],
  };
}

}
