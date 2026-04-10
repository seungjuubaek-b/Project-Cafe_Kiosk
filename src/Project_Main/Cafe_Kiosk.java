package Project_Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// 3. 메인 키오스크 클래스
public class Cafe_Kiosk {
  private Scanner sc = new Scanner(System.in);
  private List<CartItem> cart = new ArrayList<>();

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
    } catch (SQLException e) {
      e.printStackTrace();
    }

    System.out.println("==================================================");
    System.out.println(" ☕ 어서오세요~ 코스타커피입니다! ☕  [담당 직원: " + clerkName + "]");
    System.out.println("==================================================");
    showStartScreen();
  }

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
        System.out.println("\n❌ 잘못 선택하셨습니다. 다시 선택해주세요.");
      }
    }
  }

  private void showCategoryScreen() {
    while (true) {
      System.out.println("\n--------------------------------------------------");
      System.out.println("📋 카테고리를 선택해주세요.");
      try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
           Statement stmt = conn.createStatement();
           ResultSet rs = stmt.executeQuery("SELECT * FROM category")) {
        List<String> categoryIds = new ArrayList<>();
        System.out.print("  ");
        while (rs.next()) {
          String id = String.valueOf(rs.getInt("categoryid"));
          categoryIds.add(id);
          System.out.print("[" + id + "] " + rs.getString("category_name") + "   ");
        }
        System.out.println("\n  [4] 다음(결제)   [0] 이전");
        System.out.print("\n👉 선택 : ");
        String choice = sc.nextLine();
        if (choice.equals("0")) return;
        if (choice.equals("4")) {
          if (cart.isEmpty()) {
            System.out.println("\n❌ 담긴 메뉴가 없습니다. 메뉴를 먼저 선택해주세요.");
            continue;
          }
          showPaymentConfirmScreen();
          return;
        }
        if (categoryIds.contains(choice)) {
          showMenuFromDB(choice);
        } else {
          System.out.println("\n❌ 잘못 선택하셨습니다. 다시 선택해주세요.");
        }
      } catch (SQLException e) { e.printStackTrace(); }
    }
  }

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
        String input = sc.nextLine();
        int choice = Integer.parseInt(input);
        if (choice == 0) return;
        if (choice > 0 && choice <= products.size()) {
          showQuantityScreen(products.get(choice - 1));
          return;
        } else {
          System.out.println("\n❌ 잘못 선택하셨습니다. 다시 선택해주세요.");
        }
      } catch (SQLException | NumberFormatException e) {
        System.out.println("\n❌ 잘못된 입력입니다.");
      }
    }
  }

  private void showQuantityScreen(Product product) {
    System.out.println("\n--------------------------------------------------");
    System.out.print("📦 [" + product.name + "] 수량을 입력해주세요 : ");
    int qty;
    try {
      qty = Integer.parseInt(sc.nextLine());
    } catch (NumberFormatException e) {
      System.out.println("\n❌ 잘못된 수량입니다.");
      return;
    }
    cart.add(new CartItem(product, qty));
    while (true) {
      System.out.println("\n✅ [" + product.name + " " + qty + "개]를 장바구니에 담았습니다.");
      System.out.println("\n--- 🛒 현재 장바구니 현황 ---");
      int tempTotal = calculateTotal();
      for (CartItem item : cart) {
        System.out.println(" • " + item.product.name + " (" + item.quantity + "개) : " + item.getTotalPrice() + "원");
      }
      System.out.println("---------------------------");
      System.out.println("현재 총 합계: " + tempTotal + "원");
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
        if (!cart.isEmpty()) cart.remove(cart.size() - 1);
        return;
      } else {
        System.out.println("\n❌ 잘못 선택하셨습니다.");
      }
    }
  }

  private int calculateTotal() {
    int total = 0;
    for (CartItem item : cart) { total += item.getTotalPrice(); }
    return total;
  }

  private void showPaymentConfirmScreen() {
    while (true) {
      int totalOrderPrice = calculateTotal();
      System.out.println("\n==================================================");
      System.out.println("💳 결제 확인 (주문 내역)");
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
        return;
      } else if (choice.equals("2")) {
        cart.clear();
        showStartScreen();
        return;
      }
    }
  }

  private void showPaymentMethodScreen() {
// 루프 밖으로 빼서 포인트 사용 후에도 금액이 유지되도록 수정
    int totalOriginalPrice = calculateTotal();
    int remainPrice = totalOriginalPrice;
    int usedPointAmount = 0;
    int custId = -1;
    String customerPhone = "";

    while (true) {
      System.out.println("\n--------------------------------------------------");
      System.out.println("💵 결제 수단을 선택해주세요. (남은 금액: " + remainPrice + "원)");
      System.out.println("  [1] 카드   [2] 현금   [3] 포인트   [0] 이전");
      System.out.print("👉 선택 : ");
      String choice = sc.nextLine();

      if (choice.equals("3")) {
        System.out.print("\n📱 핸드폰 번호 입력 (예: 010-1234-5678) : ");
        String phone = sc.nextLine();
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
          handlePointExpiration(conn, phone);

          String sql = "SELECT custid, total_point FROM customer WHERE phone = ?";
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, phone);
          ResultSet rs = pstmt.executeQuery();
          if (rs.next()) {
            custId = rs.getInt("custid");
            customerPhone = phone;
            int totalPoint = rs.getInt("total_point");
            System.out.println("\n⭐ [포인트 조회] 현재 보유 포인트: " + totalPoint + "P");
            if (totalPoint >= 2000) {
              int maxUsable = (totalPoint / 100) * 100;
              maxUsable = Math.min(maxUsable, remainPrice);
              System.out.print("✨ " + maxUsable + "포인트를 사용하시겠습니까? (1. 예 / 2. 아니요) : ");
              if (sc.nextLine().equals("1")) {
                usedPointAmount += maxUsable; // 누적 사용 가능하도록 수정
                remainPrice -= maxUsable;
                System.out.println("✅ 포인트 적용됨. 남은 금액: " + remainPrice + "원");
                if (remainPrice == 0) {
                  processFinalPayment("포인트전액", usedPointAmount, totalOriginalPrice, custId, customerPhone);
                  return;
                }
              }
            } else {
              System.out.println("⚠️ 포인트는 2000점 이상부터 사용 가능합니다.");
            }
          } else {
            System.out.println("\n조회된 회원이 없습니다.");
          }
        } catch (SQLException e) { e.printStackTrace(); }
      } else if (choice.equals("1") || choice.equals("2")) {
        String method = choice.equals("1") ? "카드" : "현금";
        processFinalPayment(method, usedPointAmount, totalOriginalPrice, custId, customerPhone);
        return;
      } else if (choice.equals("0")) {
        showPaymentConfirmScreen();
        return;
      }
    }
  }

  private void handlePointExpiration(Connection conn, String phone) {
    try {
      String findExpiredSql =
          "SELECT ph.point_amount, ph.expire_date, ph.custid " +
              "FROM pointhistory ph " +
              "JOIN customer c ON ph.custid = c.custid " +
              "WHERE c.phone = ? AND ph.point_type = '적립' " +
              "AND ph.expire_date <= NOW() " +
              "AND NOT EXISTS ( " +
              "    SELECT 1 FROM pointhistory ph2 " +
              "    WHERE ph2.custid = ph.custid " +
              "    AND ph2.point_type = '소멸' " +
              "    AND ph2.expire_date = ph.expire_date " +
              ")";

      PreparedStatement pstmt = conn.prepareStatement(findExpiredSql);
      pstmt.setString(1, phone);
      ResultSet rs = pstmt.executeQuery();

      while (rs.next()) {
        int expiredAmount = rs.getInt("point_amount");
        Date expireDate = rs.getDate("expire_date");
        int currentCustId = rs.getInt("custid");

        String updateTotalSql = "UPDATE customer SET total_point = total_point - ? WHERE custid = ?";
        PreparedStatement pstmtUpd = conn.prepareStatement(updateTotalSql);
        pstmtUpd.setInt(1, expiredAmount);
        pstmtUpd.setInt(2, currentCustId);
        pstmtUpd.executeUpdate();

        String insertHistorySql =
            "INSERT INTO pointhistory (custid, paymentid, point_amount, point_type, date, expire_date) " +
                "VALUES (?, NULL, ?, '소멸', NOW(), ?)";
        PreparedStatement pstmtIns = conn.prepareStatement(insertHistorySql);
        pstmtIns.setInt(1, currentCustId);
        pstmtIns.setInt(2, expiredAmount);
        pstmtIns.setDate(3, expireDate);
        pstmtIns.executeUpdate();
      }
    } catch (SQLException e) {}
  }

  // [수정 핵심] 사용한 포인트를 제외한 실결제액에서 5% 적립 로직 적용
  private void processFinalPayment(String paymentMethod, int usedPoint, int totalOriginalPrice, int existingCustId, String customerPhone) {
    int earnedPoint = 0;
    int custId = existingCustId;
    String phone = customerPhone;

    System.out.println("\n🎉 결제가 성공적으로 완료되었습니다!");

    // 1. 포인트 전액 결제인 경우 적립 절차를 완전히 건너뜀
    if (paymentMethod.equals("포인트전액")) {
      System.out.println("ℹ️ 포인트 전액 결제 시에는 추가 적립이 불가능합니다.");
    } else {
      while (true) {
        System.out.print("포인트를 적립하시겠습니까? (1. 예 / 2. 아니요) : ");
        String earnChoice = sc.nextLine();
        if (earnChoice.equals("1")) {
          while (true) {
            System.out.print("\n📱 적립할 핸드폰 번호 입력 (예: 010-1234-5678) : ");
            phone = sc.nextLine();
            custId = findMemberByPhone(phone);

            if (custId == -1) {
              System.out.print("❓ 신규 회원으로 등록하시겠습니까? (1. 예 / 2. 아니요) : ");
              String regChoice = sc.nextLine();
              if (regChoice.equals("1")) {
                custId = createNewCustomer(phone);
                System.out.println("\n✨ [신규 회원 등록] 코스타커피의 새로운 회원이 되신 것을 환영합니다!");
                break;
              } else {
                System.out.println("다시 번호를 입력해 주세요.");
                continue;
              }
            } else { break; }
          }
          // 2. 복합 결제 시 (총 주문 금액 - 사용한 포인트)의 5%만 적립
          int actualPaidAmount = totalOriginalPrice - usedPoint;
          earnedPoint = (int) (actualPaidAmount * 0.05);
          System.out.println("✅ " + earnedPoint + "P가 적립되었습니다.");
          break;
        } else if (earnChoice.equals("2")) { break; }
      }
    }

    saveOrderToDB(paymentMethod, usedPoint, earnedPoint, totalOriginalPrice, custId);

    System.out.println("\n==================================================");
    System.out.println(" 🥰 감사합니다~ 좋은 하루 보내세요! ");
    System.out.println("==================================================");
    System.exit(0);
  }

  private int findMemberByPhone(String phone) {
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
      String sqlFind = "SELECT custid FROM customer WHERE phone = ?";
      PreparedStatement pstmt = conn.prepareStatement(sqlFind);
      pstmt.setString(1, phone);
      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) return rs.getInt("custid");
    } catch (SQLException e) { e.printStackTrace(); }
    return -1;
  }

  private int createNewCustomer(String phone) {
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
      String sqlInsert = "INSERT INTO customer (phone, total_point) VALUES (?, 0)";
      PreparedStatement pstmtIns = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
      pstmtIns.setString(1, phone);
      pstmtIns.executeUpdate();
      ResultSet rsKeys = pstmtIns.getGeneratedKeys();
      if (rsKeys.next()) return rsKeys.getInt(1);
    } catch (SQLException e) { e.printStackTrace(); }
    return -1;
  }

  private void saveOrderToDB(String paymentMethod, int usedPoint, int earnedPoint, int totalOriginalPrice, int custId) {
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

        String insertPayment = "INSERT INTO payment (custid, cartid, total_price, payment_date, payment_method, earned_point, used_point, expired_point) " +
            "VALUES (?, ?, ?, NOW(), ?, ?, ?, 0)";
        PreparedStatement pPay = conn.prepareStatement(insertPayment, Statement.RETURN_GENERATED_KEYS);
        pPay.setInt(1, custId);
        pPay.setInt(2, lastCartId);
        pPay.setInt(3, totalOriginalPrice);
        pPay.setString(4, paymentMethod);
        pPay.setInt(5, earnedPoint);
        pPay.setInt(6, usedPoint);
        pPay.executeUpdate();

        ResultSet rsPay = pPay.getGeneratedKeys();
        int paymentId = 0;
        if (rsPay.next()) paymentId = rsPay.getInt(1);

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
      } catch (Exception ex) { conn.rollback(); ex.printStackTrace(); }
    } catch (SQLException e) { e.printStackTrace(); }
  }

  public static void main(String[] args) { new Cafe_Kiosk().start(); }
}