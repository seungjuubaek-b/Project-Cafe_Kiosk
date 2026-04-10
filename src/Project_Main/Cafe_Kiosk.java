package Project_Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// 3. 메인 키오스크 클래스
public class Cafe_Kiosk {
  private Scanner sc = new Scanner(System.in);
  private List<CartItem> cart = new ArrayList<>();

  // DB 연결 설정
  private final String URL = "jdbc:mysql://localhost:3306/cafe_kiosk";
  private final String USER = "root";
  private final String PASSWORD = "1111";

  public void start() {
    String clerkName = "직원";
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT clerkname FROM clerk LIMIT 1")) {
      if (rs.next()) {
        clerkName = rs.getString("clerkname");
      }
    } catch (SQLException e) { }

    System.out.println("==================================================");
    System.out.println(" ☕ 어서오세요~ 코스타커피입니다! ☕  [담당 직원: " + clerkName + "]");
    System.out.println("==================================================");
    showStartScreen();
  }

  // [화면 1] 주문하시겠습니까?
  private void showStartScreen() {
    while (true) {
      System.out.println("\n▶ 주문하시겠습니까?");
      System.out.println("  [1] 네   [2] 아니요");
      System.out.print("👉 선택 : ");
      String choice = sc.nextLine();

      if (choice.equals("1")) {
        cart.clear();
        showCategoryScreen();
      } else if (choice.equals("2")) {
        System.out.println("\n프로그램을 종료합니다.");
        System.exit(0);
      } else {
        System.out.println("\n❌ 잘못된 입력입니다. 다시 선택해주세요.");
      }
    }
  }

  // [화면 2] 카테고리 선택
  private void showCategoryScreen() {
    while (true) {
      System.out.println("\n--------------------------------------------------");
      System.out.println("📋 카테고리를 선택해주세요.");
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT * FROM category")) {

        System.out.print("  ");
        while (rs.next()) {
          System.out.print("[" + rs.getInt("categoryid") + "] " + rs.getString("category_name") + "   ");
        }
        System.out.println("[0] 이전"); // 이름 수정: 이전/뒤로 -> 이전

        System.out.print("\n👉 선택 : ");
        String choice = sc.nextLine();
        if (choice.equals("0")) return;
        showMenuFromDB(choice);
      } catch (SQLException e) {
        System.out.println("DB 연결 오류: " + e.getMessage());
      }
    }
  }

  // [화면 3] 메뉴 선택
  private void showMenuFromDB(String categoryId) {
    while (true) {
      List<Product> products = new ArrayList<>();
      System.out.println("\n--------------------------------------------------");
      System.out.println("🥤 [메뉴를 선택해주세요]");

      String sql = "SELECT productid, product_name, price FROM product WHERE categoryid = ?";

      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
           PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, categoryId);
        ResultSet rs = pstmt.executeQuery();

        int i = 1;
        while (rs.next()) {
          Product p = new Product(rs.getInt("productid"), rs.getString("product_name"), rs.getInt("price"));
          products.add(p);
          System.out.println("  [" + i + "] " + p.name + " (" + p.price + "원)");
          i++;
        }
        System.out.println("  [0] 이전");

        System.out.print("\n👉 메뉴 번호 입력 : ");
        int choice = Integer.parseInt(sc.nextLine());
        if (choice == 0) return;

        if (choice > 0 && choice <= products.size()) {
          showQuantityScreen(products.get(choice - 1));
          return;
        } else {
          System.out.println("\n❌ 잘못된 메뉴 번호입니다.");
        }
      } catch (SQLException | NumberFormatException e) {
        System.out.println("\n❌ 입력을 확인해주세요.");
      }
    }
  }

  // [화면 4] 수량 입력 및 장바구니 확인
  private void showQuantityScreen(Product product) {
    while (true) {
      System.out.println("\n--------------------------------------------------");
      System.out.print("📦 [" + product.name + "] 수량을 입력해주세요 : ");
      int qty = Integer.parseInt(sc.nextLine());

      // 장바구니에 우선 추가
      cart.add(new CartItem(product, qty));

      System.out.println("\n✅ [" + product.name + " " + qty + "개]를 장바구니에 담았습니다.");
      System.out.println("\n--- 🛒 현재 장바구니 현황 ---");
      int tempTotal = 0;
      for(CartItem item : cart) {
        System.out.println(" • " + item.product.name + " (" + item.quantity + "개) : " + item.getTotalPrice() + "원");
        tempTotal += item.getTotalPrice();
      }
      System.out.println("---------------------------");
      System.out.println("현재 총 합계: " + tempTotal + "원");

      // 사용자 요청에 따른 텍스트 및 로직 수정
      System.out.println("\n  [1] 추가 주문하기   [2] 결제하기   [0] 이전");
      System.out.print("👉 선택 : ");
      String choice = sc.nextLine();

      if (choice.equals("1")) {
        showCategoryScreen();
        return;
      } else if (choice.equals("2")) {
        showPaymentConfirmScreen();
        return;
      } else if (choice.equals("0")) {
        // [로직 추가] 수량 수정을 위해 방금 담은 아이템을 리스트에서 제거하고 메뉴판으로 복귀
        if(!cart.isEmpty()) cart.remove(cart.size() - 1);
        return;
      }
    }
  }

  private int calculateTotal() {
    int total = 0;
    for (CartItem item : cart) {
      total += item.getTotalPrice();
    }
    return total;
  }

  // [화면 5] 결제 확인
  private void showPaymentConfirmScreen() {
    int totalOrderPrice = calculateTotal();
    System.out.println("\n==================================================");
    System.out.println("💳 결제 화면으로 이동합니다. 주문 내역을 확인해주세요.");
    for (CartItem item : cart) {
      System.out.println(" - " + item.product.name + " x " + item.quantity + " : " + item.getTotalPrice() + "원");
    }
    System.out.println("--------------------------------------------------");
    System.out.println("💰 최종 결제 금액 : " + totalOrderPrice + "원");
    System.out.println("--------------------------------------------------");
    System.out.print("결제하시겠습니까? (1. 네 / 2. 아니요) : ");

    String choice = sc.nextLine();
    if (choice.equals("1")) {
      showPaymentMethodScreen();
    } else {
      System.out.println("\n❌ 결제가 취소되었습니다. 초기화면으로 돌아갑니다.");
      cart.clear();
      showStartScreen();
    }
  }

  // [화면 6] 결제 수단 선택
  private void showPaymentMethodScreen() {
    int remainPrice = calculateTotal();
    int usedPointAmount = 0;
    int custId = -1;
    String customerPhone = "";

    while (true) {
      System.out.println("\n--------------------------------------------------");
      System.out.println("💵 결제 수단을 선택해주세요. (남은 결제 금액: " + remainPrice + "원)");
      System.out.println("  [1] 카드   [2] 현금   [3] 포인트   [0] 이전");
      System.out.print("👉 선택 : ");
      String choice = sc.nextLine();

      if (choice.equals("3")) {
        if (usedPointAmount > 0) {
          System.out.println("\n⚠️ 이미 포인트를 적용하셨습니다.");
          continue;
        }

        System.out.print("\n📱 핸드폰 번호를 입력해주세요 : ");
        String phone = sc.nextLine();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
          String sql = "SELECT custid, total_point FROM customer WHERE phone = ?";
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, phone);
          ResultSet rs = pstmt.executeQuery();

          if (rs.next()) {
            custId = rs.getInt("custid");
            customerPhone = phone;
            int totalPoint = rs.getInt("total_point");
            showPointInfo(conn, custId, totalPoint);

            if (totalPoint >= 2000) {
              int maxUsable = (totalPoint / 100) * 100;
              maxUsable = Math.min(maxUsable, remainPrice);

              System.out.print("✨ " + maxUsable + "포인트를 사용하시겠습니까? (1. 예 / 2. 아니요) : ");
              String pointChoice = sc.nextLine();

              if (pointChoice.equals("1")) {
                usedPointAmount = maxUsable;
                remainPrice -= usedPointAmount;
                System.out.println("\n✅ 포인트 적용 완료. 남은 결제 금액: " + remainPrice + "원");

                if (remainPrice == 0) {
                  processFinalPayment("포인트전액", usedPointAmount, remainPrice, custId, customerPhone);
                  return;
                }
              }
            } else {
              System.out.println("\n⚠️ 포인트는 2000점 이상부터 사용 가능합니다.");
            }
          } else {
            System.out.println("\n조회된 회원이 없어 신규 가입 처리됩니다.");
            custId = findOrCreateCustomer(phone);
            customerPhone = phone;
          }
        } catch (SQLException e) { }
      }
      else if (choice.equals("1") || choice.equals("2")) {
        String method = choice.equals("1") ? "카드" : "현금";
        processFinalPayment(method, usedPointAmount, remainPrice, custId, customerPhone);
        return;
      } else if (choice.equals("0")) {
        showPaymentConfirmScreen(); // 이전 단계인 결제 확인 화면으로 이동
        return;
      }
    }
  }

  private void showPointInfo(Connection conn, int custId, int totalPoint) throws SQLException {
    System.out.println("\n⭐ --- [포인트 조회 결과] --- ⭐");
    System.out.println("▶ 누적 포인트: " + totalPoint + "원");
    System.out.println("------------------------------");
  }

  private void processFinalPayment(String paymentMethod, int usedPointAmount, int paidAmount, int existingCustId, String customerPhone) {
    int earnedPoint = 0;
    int custId = existingCustId;
    String phone = customerPhone;

    System.out.println("\n🎉 결제가 성공적으로 완료되었습니다! 🎉");
    System.out.print("포인트를 적립하시겠습니까? (1. 예 / 2. 아니요) : ");
    String earnChoice = sc.nextLine();

    if (earnChoice.equals("1")) {
      if (custId == -1) {
        System.out.print("\n📱 적립할 핸드폰 번호를 입력해주세요 : ");
        phone = sc.nextLine();
        custId = findOrCreateCustomer(phone);
      }
      earnedPoint = (int)(calculateTotal() * 0.05);
      System.out.println("\n✅ 포인트 " + earnedPoint + "점이 적립되었습니다.");
    }

    saveOrderToDB(paymentMethod, usedPointAmount, earnedPoint, paidAmount, custId);

    System.out.println("\n==================================================");
    System.out.println(" 🥰 감사합니다~ 좋은 하루 보내세요! 또 오세요! ☕🍰 ");
    System.out.println("==================================================");
    System.exit(0);
  }

  private int findOrCreateCustomer(String phone) {
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
      String sqlFind = "SELECT custid FROM customer WHERE phone = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sqlFind)) {
        pstmt.setString(1, phone);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) return rs.getInt("custid");
      }
      String sqlInsert = "INSERT INTO customer (phone, total_point) VALUES (?, 0)";
      try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
        pstmt.setString(1, phone);
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);
      }
    } catch (SQLException e) { }
    return -1;
  }

  private void saveOrderToDB(String paymentMethod, int usedPoint, int earnedPoint, int paidAmount, int custId) {
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
      conn.setAutoCommit(false);
      try {
        if (custId == -1) {
          String insertCustomer = "INSERT INTO customer (phone, total_point) VALUES (NULL, 0)";
          PreparedStatement pCust = conn.prepareStatement(insertCustomer, Statement.RETURN_GENERATED_KEYS);
          pCust.executeUpdate();
          ResultSet rsC = pCust.getGeneratedKeys();
          if (rsC.next()) custId = rsC.getInt(1);
        } else {
          String updateCust = "UPDATE customer SET total_point = total_point - ? + ? WHERE custid = ?";
          PreparedStatement pUpd = conn.prepareStatement(updateCust);
          pUpd.setInt(1, usedPoint);
          pUpd.setInt(2, earnedPoint);
          pUpd.setInt(3, custId);
          pUpd.executeUpdate();
        }

        int lastCartId = 0;
        for (CartItem item : cart) {
          String insertCart = "INSERT INTO cart (custid, proudctid, quantity) VALUES (?, ?, ?)";
          PreparedStatement pCart = conn.prepareStatement(insertCart, Statement.RETURN_GENERATED_KEYS);
          pCart.setInt(1, custId);
          pCart.setInt(2, item.product.id);
          pCart.setInt(3, item.quantity);
          pCart.executeUpdate();
          ResultSet rsCart = pCart.getGeneratedKeys();
          if (rsCart.next()) lastCartId = rsCart.getInt(1);
        }

        String insertPayment = "INSERT INTO payment (custid, cartid, total_price, payment_date, payment_method, earned_point, used_point, expired_point) VALUES (?, ?, ?, NOW(), ?, ?, ?, 0)";
        PreparedStatement pPay = conn.prepareStatement(insertPayment, Statement.RETURN_GENERATED_KEYS);
        pPay.setInt(1, custId);
        pPay.setInt(2, lastCartId);
        pPay.setInt(3, calculateTotal());
        pPay.setString(4, paymentMethod);
        pPay.setInt(5, earnedPoint);
        pPay.setInt(6, usedPoint);
        pPay.executeUpdate();

        ResultSet rsPay = pPay.getGeneratedKeys();
        int paymentId = 0;
        if(rsPay.next()) paymentId = rsPay.getInt(1);

        if (usedPoint > 0) {
          String insertHistory = "INSERT INTO pointhistory (custid, paymentid, point_amount, point_type, date, expire_date) VALUES (?, ?, ?, '사용', NOW(), NULL)";
          PreparedStatement pHist = conn.prepareStatement(insertHistory);
          pHist.setInt(1, custId);
          pHist.setInt(2, paymentId);
          pHist.setInt(3, usedPoint);
          pHist.executeUpdate();
        }

        if (earnedPoint > 0) {
          String insertHistory = "INSERT INTO pointhistory (custid, paymentid, point_amount, point_type, date, expire_date) VALUES (?, ?, ?, '적립', NOW(), DATE_ADD(NOW(), INTERVAL 6 MONTH))";
          PreparedStatement pHist = conn.prepareStatement(insertHistory);
          pHist.setInt(1, custId);
          pHist.setInt(2, paymentId);
          pHist.setInt(3, earnedPoint);
          pHist.executeUpdate();
        }

        conn.commit();
      } catch (Exception ex) {
        conn.rollback();
        throw ex;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      System.out.println("DB 저장 오류: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    new Cafe_Kiosk().start();
  }
}