package Project_Main;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// 3. 메인 키오스크 클래스
public class Cafe_Kiosk {
  private Scanner sc = new Scanner(System.in);
  private CartItem currentItem = null;

  // DB 연결 설정 (본인의 DB 정보로 수정하세요)
  private final String URL = "jdbc:mysql://localhost:3306/cafe_kiosk";
  private final String USER = "root";
  private final String PASSWORD = "1111"; // 본인 DB 비밀번호

  public void start() {
    String clerkName = "직원";
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT clerkname FROM clerk LIMIT 1")) {
      if (rs.next()) {
        clerkName = rs.getString("clerkname");
      }
    } catch (SQLException e) {
      // 오류 무시
    }

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
        System.out.println("[0] 이전");

        System.out.print("\n👉 선택 : ");
        String choice = sc.nextLine();
        if (choice.equals("0")) return;
        showMenuFromDB(choice);
      } catch (SQLException e) {
        System.out.println("DB 연결 오류: " + e.getMessage());
      }
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
        int choice = Integer.parseInt(sc.nextLine());
        if (choice == 0) return;

        if (choice > 0 && choice <= products.size()) {
          showQuantityScreen(products.get(choice - 1));
          return;
        } else {
          System.out.println("\n❌ 잘못된 메뉴 번호입니다.");
        }
      } catch (SQLException e) {
        System.out.println("메뉴 조회 오류: " + e.getMessage());
      } catch (NumberFormatException e) {
        System.out.println("\n❌ 숫자를 입력해주세요.");
      }
    }
  }

  // [화면 4] 수량 입력 및 장바구니 담기
  private void showQuantityScreen(Product product) {
    while (true) {
      System.out.println("\n--------------------------------------------------");
      System.out.print("📦 [" + product.name + "] 수량을 입력해주세요 : ");
      int qty = Integer.parseInt(sc.nextLine());

      System.out.println("\n  [1] 장바구니 담기   [9] 처음으로   [0] 이전");
      System.out.print("👉 선택 : ");
      String choice = sc.nextLine();

      if (choice.equals("1")) {
        currentItem = new CartItem(product, qty);
        System.out.println("\n✅ 장바구니에 담았습니다.");
        System.out.println("  [상품: " + currentItem.product.name + " | 수량: " + currentItem.quantity + "개 | 금액: " + currentItem.getTotalPrice() + "원]");
        showPaymentConfirmScreen();
        return;
      } else if (choice.equals("9")) {
        showStartScreen();
      } else if (choice.equals("0")) {
        return;
      }
    }
  }

  // [화면 5] 결제 확인
  private void showPaymentConfirmScreen() {
    System.out.println("\n==================================================");
    System.out.println("💳 결제 화면으로 이동합니다. 금액을 확인해주세요.");
    System.out.println("💰 총 결제 금액 : " + currentItem.getTotalPrice() + "원");
    System.out.println("--------------------------------------------------");
    System.out.print("결제하시겠습니까? (1. 네 / 2. 아니요) : ");

    String choice = sc.nextLine();
    if (choice.equals("1")) {
      showPaymentMethodScreen();
    } else {
      System.out.println("\n❌ 결제가 취소되었습니다.");
      showStartScreen();
    }
  }

  // [화면 6] 결제 수단 및 포인트 처리 화면
  private void showPaymentMethodScreen() {
    int remainPrice = currentItem.getTotalPrice(); // 남은 결제 금액
    int usedPointAmount = 0; // 사용한 포인트
    int custId = -1; // 고객 ID
    String customerPhone = "";

    while (true) {
      System.out.println("\n--------------------------------------------------");
      System.out.println("💵 결제 수단을 선택해주세요. (남은 결제 금액: " + remainPrice + "원)");
      System.out.println("  [1] 카드   [2] 현금   [3] 포인트   [9] 처음으로   [0] 이전");
      System.out.print("👉 선택 : ");
      String choice = sc.nextLine();

      // 1. 포인트 사용 프로세스
      if (choice.equals("3")) {
        if (usedPointAmount > 0) {
          System.out.println("\n⚠️ 이미 포인트를 적용하셨습니다. 나머지 금액에 대한 결제 수단을 선택해주세요.");
          continue;
        }

        System.out.print("\n📱 핸드폰 번호를 입력해주세요 (예: 010-1234-5678) : ");
        String phone = sc.nextLine();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
          String sql = "SELECT custid, total_point FROM customer WHERE phone = ?";
          PreparedStatement pstmt = conn.prepareStatement(sql);
          pstmt.setString(1, phone);
          ResultSet rs = pstmt.executeQuery();

          if (rs.next()) {
            // 기존 회원인 경우
            custId = rs.getInt("custid");
            customerPhone = phone;
            int totalPoint = rs.getInt("total_point");

            showPointInfo(conn, custId, totalPoint);

            // 사용 조건 검사 (2000원 이상, 100원 단위)
            if (totalPoint >= 2000) {
              int maxUsable = (totalPoint / 100) * 100;
              maxUsable = Math.min(maxUsable, remainPrice);

              System.out.print("✨ " + maxUsable + "포인트를 사용하시겠습니까? (1. 예 / 2. 아니요) : ");
              String pointChoice = sc.nextLine();

              if (pointChoice.equals("1")) {
                usedPointAmount = maxUsable;
                remainPrice -= usedPointAmount;
                System.out.println("\n✅ 포인트가 사용되었습니다. 현재 포인트 잔액은 " + (totalPoint - usedPointAmount) + "원 입니다.");

                if (remainPrice == 0) {
                  processFinalPayment("포인트전액", usedPointAmount, remainPrice, custId, customerPhone);
                  return;
                } else {
                  System.out.println("\n남은 결제 금액 " + remainPrice + "원을 결제해주세요.");
                  continue;
                }
              }
            } else {
              System.out.println("\n⚠️ 포인트는 2000점 이상부터 사용 가능합니다. (현재 잔액: " + totalPoint + "점)");
            }
          } else {
            // 회원이 없을 경우 즉시 신규 고객으로 등록
            System.out.println("\n조회된 회원이 없어 신규 고객으로 자동 등록합니다.");
            custId = findOrCreateCustomer(phone); // DB에 회원 Insert
            customerPhone = phone;
            System.out.println("🎉 신규 가입을 환영합니다! 현재 보유 포인트가 0점 이므로 포인트 사용은 불가합니다.");
            System.out.println("결제 후 총 결제 금액의 5%가 자동 적립됩니다.");
            // 다음 결제수단(카드/현금)을 선택할 수 있도록 루프 계속 진행
          }
        } catch (SQLException e) {
          System.out.println("DB 연결 오류: " + e.getMessage());
        }

      }
      // 2. 일반 카드/현금 결제 프로세스
      else if (choice.equals("1") || choice.equals("2")) {
        String method = choice.equals("1") ? "카드" : "현금";
        processFinalPayment(method, usedPointAmount, remainPrice, custId, customerPhone);
        return;
      } else if (choice.equals("9")) {
        showStartScreen();
        return;
      } else if (choice.equals("0")) {
        showQuantityScreen(currentItem.product);
        return;
      } else {
        System.out.println("\n❌ 잘못된 입력입니다.");
      }
    }
  }

  // [추가 기능] 고객의 포인트 내역 요약 출력
  private void showPointInfo(Connection conn, int custId, int totalPoint) throws SQLException {
    System.out.println("\n⭐ --- [포인트 조회 결과] --- ⭐");
    System.out.println("▶ 누적 포인트: " + totalPoint + "원");

    // 1. 오늘 적립한 포인트 조회
    String sqlToday = "SELECT IFNULL(SUM(point_amount), 0) FROM pointhistory WHERE custid = ? AND point_type = '적립' AND DATE(date) = CURDATE()";
    try (PreparedStatement pstmt = conn.prepareStatement(sqlToday)) {
      pstmt.setInt(1, custId);
      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
        System.out.println("▶ 오늘 적립한 포인트: " + rs.getInt(1) + "원");
      }
    }

    // 2. 유효기간 (DB의 expire_date 컬럼 활용하여 오늘 이후 만료되는 내역 조회)
    String sqlValid = "SELECT point_amount, expire_date FROM pointhistory WHERE custid = ? AND point_type = '적립' AND expire_date >= CURDATE() ORDER BY expire_date ASC LIMIT 3";
    try (PreparedStatement pstmt = conn.prepareStatement(sqlValid)) {
      pstmt.setInt(1, custId);
      ResultSet rs = pstmt.executeQuery();
      System.out.println("\n[다가오는 포인트 소멸 예정일]");
      boolean hasHistory = false;
      while (rs.next()) {
        hasHistory = true;
        // 날짜가 보기 좋게 출력되도록 년-월-일만 자르기
        String expDate = rs.getString("expire_date");
        if(expDate != null && expDate.length() >= 10) {
          expDate = expDate.substring(0, 10);
        }
        System.out.println(" - " + expDate + "까지: " + rs.getInt("point_amount") + "원");
      }
      if (!hasHistory) {
        System.out.println(" - 소멸 예정인 포인트 내역이 없습니다.");
      }
    }
    System.out.println("------------------------------");
  }

  // 최종 결제 확정 및 포인트 적립 프로세스
  private void processFinalPayment(String paymentMethod, int usedPointAmount, int paidAmount, int existingCustId, String customerPhone) {
    int earnedPoint = 0;
    int custId = existingCustId;
    String phone = customerPhone;

    System.out.println("\n🎉 결제가 성공적으로 완료되었습니다! 🎉");

    // [수정된 부분] 결제 수단(포인트/카드/현금)과 상관없이 항상 적립 여부를 묻기
    System.out.print("포인트를 적립하시겠습니까? (1. 예 / 2. 아니요) : ");
    String earnChoice = sc.nextLine();

    if (earnChoice.equals("1")) {
      // Option 3번을 거치지 않아 custId가 없는 경우에만 번호를 다시 물어봄
      if (custId == -1) {
        System.out.print("\n📱 적립할 핸드폰 번호를 입력해주세요 (예: 010-1234-5678) : ");
        phone = sc.nextLine();
        custId = findOrCreateCustomer(phone);
      }

      // [수정된 부분] 포인트는 결제한 수단 상관없이 '총 주문 금액'의 5%를 적립
      earnedPoint = (int)(currentItem.getTotalPrice() * 0.05);
      System.out.println("\n✅ 포인트 " + earnedPoint + "점이 적립되었습니다.");

      // 최신 잔액 계산하여 출력 (기존 + 적립 - 사용)
      int currentTotal = getCustomerTotalPoint(custId);
      System.out.println("   [현재 포인트 잔액은 " + (currentTotal - usedPointAmount + earnedPoint) + "점 입니다.]");
    }

    saveOrderToDB(paymentMethod, usedPointAmount, earnedPoint, paidAmount, custId);

    System.out.println("\n==================================================");
    System.out.println(" 🥰 감사합니다~ 좋은 하루 보내세요! 또 오세요! ☕🍰 ");
    System.out.println("==================================================");
    System.exit(0);
  }

  // 회원 조회 또는 신규 생성 (없으면 새로 DB에 Insert)
  private int findOrCreateCustomer(String phone) {
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
      String sqlFind = "SELECT custid FROM customer WHERE phone = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sqlFind)) {
        pstmt.setString(1, phone);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
          return rs.getInt("custid");
        }
      }
      // 회원이 없다면 새로 생성
      String sqlInsert = "INSERT INTO customer (phone, total_point) VALUES (?, 0)";
      try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
        pstmt.setString(1, phone);
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) return rs.getInt(1);
      }
    } catch (SQLException e) {
      System.out.println("회원 조회/생성 오류: " + e.getMessage());
    }
    return -1;
  }

  private int getCustomerTotalPoint(int custId) {
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
      String sql = "SELECT total_point FROM customer WHERE custid = ?";
      try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, custId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) return rs.getInt("total_point");
      }
    } catch (SQLException e) { /* 무시 */ }
    return 0;
  }

  // 모든 결제 및 포인트 내역을 DB에 저장 (트랜잭션 적용)
  private void saveOrderToDB(String paymentMethod, int usedPoint, int earnedPoint, int paidAmount, int custId) {
    try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
      conn.setAutoCommit(false); // 트랜잭션 시작

      try {
        // 1. 비회원인 경우 임시 고객 생성
        if (custId == -1) {
          String insertCustomer = "INSERT INTO customer (phone, total_point) VALUES (NULL, 0)";
          PreparedStatement pCust = conn.prepareStatement(insertCustomer, Statement.RETURN_GENERATED_KEYS);
          pCust.executeUpdate();
          ResultSet rsC = pCust.getGeneratedKeys();
          if (rsC.next()) custId = rsC.getInt(1);
        } else {
          // 회원이면 total_point 갱신
          String updateCust = "UPDATE customer SET total_point = total_point - ? + ? WHERE custid = ?";
          PreparedStatement pUpd = conn.prepareStatement(updateCust);
          pUpd.setInt(1, usedPoint);
          pUpd.setInt(2, earnedPoint);
          pUpd.setInt(3, custId);
          pUpd.executeUpdate();
        }

        // 2. 장바구니(cart) 저장 (db의 proudctid 컬럼명 반영)
        String insertCart = "INSERT INTO cart (custid, proudctid, quantity) VALUES (?, ?, ?)";
        PreparedStatement pCart = conn.prepareStatement(insertCart, Statement.RETURN_GENERATED_KEYS);
        pCart.setInt(1, custId);
        pCart.setInt(2, currentItem.product.id);
        pCart.setInt(3, currentItem.quantity);
        pCart.executeUpdate();
        ResultSet rsCart = pCart.getGeneratedKeys();
        int cartId = 0;
        if (rsCart.next()) cartId = rsCart.getInt(1);

        // 3. 결제(payment) 정보 저장 (실제 지불한 금액, 수단, 포인트 내역 기록)
        String insertPayment = "INSERT INTO payment (custid, cartid, total_price, payment_date, payment_method, earned_point, used_point, expired_point) VALUES (?, ?, ?, NOW(), ?, ?, ?, 0)";
        PreparedStatement pPay = conn.prepareStatement(insertPayment, Statement.RETURN_GENERATED_KEYS);
        pPay.setInt(1, custId);
        pPay.setInt(2, cartId);
        pPay.setInt(3, currentItem.getTotalPrice()); // 원래 상품 가격
        pPay.setString(4, paymentMethod);
        pPay.setInt(5, earnedPoint);
        pPay.setInt(6, usedPoint);
        pPay.executeUpdate();

        ResultSet rsPay = pPay.getGeneratedKeys();
        int paymentId = 0;
        if(rsPay.next()) paymentId = rsPay.getInt(1);

        // 4. 포인트 이력(pointhistory) 저장 (여기에 만료일 처리 추가됨)
        if (usedPoint > 0) {
          // '사용' 내역은 만료일이 없으므로 NULL 저장
          String insertHistory = "INSERT INTO pointhistory (custid, paymentid, point_amount, point_type, date, expire_date) VALUES (?, ?, ?, '사용', NOW(), NULL)";
          PreparedStatement pHist = conn.prepareStatement(insertHistory);
          pHist.setInt(1, custId);
          pHist.setInt(2, paymentId);
          pHist.setInt(3, usedPoint);
          pHist.executeUpdate();
        }

        if (earnedPoint > 0) {
          // '적립' 내역은 현재 시간에 6개월을 더한 만료일(expire_date)을 함께 저장
          String insertHistory = "INSERT INTO pointhistory (custid, paymentid, point_amount, point_type, date, expire_date) VALUES (?, ?, ?, '적립', NOW(), DATE_ADD(NOW(), INTERVAL 6 MONTH))";
          PreparedStatement pHist = conn.prepareStatement(insertHistory);
          pHist.setInt(1, custId);
          pHist.setInt(2, paymentId);
          pHist.setInt(3, earnedPoint);
          pHist.executeUpdate();
        }

        conn.commit(); // 모든 저장 성공 시 트랜잭션 커밋

      } catch (Exception ex) {
        conn.rollback(); // 중간에 에러 시 롤백
        throw ex;
      } finally {
        conn.setAutoCommit(true);
      }

    } catch (SQLException e) {
      System.out.println("DB 저장 중 오류가 발생했습니다: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    new Cafe_Kiosk().start();
  }
}