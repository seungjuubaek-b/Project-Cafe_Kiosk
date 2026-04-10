package Project_Main;

// 2. 장바구니에 담긴 항목을 관리하는 클래스
public class CartItem {
  Product product;
  int quantity;

  public CartItem(Product product, int quantity) {
    this.product = product;
    this.quantity = quantity;
  }

  public int getTotalPrice() {
    return product.price * quantity;
  }
}
