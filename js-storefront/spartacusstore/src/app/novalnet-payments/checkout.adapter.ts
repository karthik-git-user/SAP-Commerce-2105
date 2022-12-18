import {
  PaymentDetails,
  CheckoutPaymentAdapter,
  CardType
} from "@spartacus/core";
import { Injectable } from "@angular/core";
import { Observable, of } from "rxjs";

@Injectable()
export class NovalnetCheckoutPaymentAdapter
  implements CheckoutPaymentAdapter {
  constructor() {}

  public create(
    userId: string,
    cartId: string,
    paymentDetails: PaymentDetails
  ): Observable<PaymentDetails> {
    // Cybersource-based logic to create payment details
  }

  // Add other methods to get endpoints, map payment fields, and so on, as needed.
}
