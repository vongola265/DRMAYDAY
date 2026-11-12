# 🏥 MEDICAL SYSTEM - QUY CHUẨN PHÁT TRIỂN & PIPELINE LÀM VIỆC NHÓM

Tài liệu này quy định kiến trúc mã nguồn, quy trình code một tính năng mới và quy chuẩn Git dành cho toàn bộ thành viên trong nhóm. **Mọi thành viên bắt buộc phải tuân thủ để tránh conflict và đảm bảo chất lượng code.**

---

## 🏛️ 1. KIẾN TRÚC N-LAYER (N-LAYER ARCHITECTURE)

Dự án tuân thủ nghiêm ngặt mô hình kiến trúc phân lớp. Dữ liệu đi một chiều từ ngoài vào trong:
`Client (Postman/Frontend)` ➔ `Controller` ➔ `Service` ➔ `Repository` ➔ `Entity` ➔ `PostgreSQL`

```
src/main/java/com/cnw/medical_system/
├── controller/     # Tầng giao tiếp: Đón nhận HTTP Request, gọi Service, trả về Response
├── service/        # Bộ não: Chứa toàn bộ Business Logic, nghiệp vụ, tính toán
├── repository/     # Tầng truy cập dữ liệu: Giao tiếp với PostgreSQL qua Spring Data JPA
├── entity/         # Ánh xạ CSDL: Đại diện 1-1 cho các bảng trong database
├── dto/            # Data Transfer Object: Dữ liệu trao đổi giữa Client và Server
│   ├── request/    # DTO nhận vào từ client (phải có validation)
│   └── response/   # DTO trả về cho client
├── mapper/         # Chuyển đổi Entity <-> DTO (dùng MapStruct)
├── exception/      # Xử lý ngoại lệ tập trung (Global Exception Handler)
└── config/         # Cấu hình hệ thống (Swagger, Security,...)
```

### ⚠️ Các nguyên tắc "bất di bất dịch":
1. **KHÔNG BAO GIỜ** trả thẳng `Entity` ra ngoài Controller. Bắt buộc phải chuyển sang `Response DTO` để bảo mật cấu trúc CSDL.
2. **Controller** không được chứa logic nghiệp vụ hay câu lệnh if/else tính toán. Controller chỉ nhận request, gọi Service và trả về `ResponseEntity`.
3. Mọi thao tác ghi (Thêm/Sửa/Xóa) ở Service phải có `@Transactional`. Các hàm chỉ đọc dữ liệu thì dùng `@Transactional(readOnly = true)`.
4. Dependency Injection: **KHÔNG dùng `@Autowired`**, luôn dùng `@RequiredArgsConstructor` kết hợp `final` field từ Lombok.

---

## 🚀 2. PIPELINE 5 BƯỚC CODE MỘT TÍNH NĂNG MỚI

Khi bạn được giao làm một phân hệ (ví dụ: Quản lý Bệnh nhân, Phiếu khám, Đơn thuốc), hãy làm đúng theo 5 bước sau:

```
[Bước 1: Entity] ➔ [Bước 2: Repository] ➔ [Bước 3: DTO] ➔ [Bước 4: Service] ➔ [Bước 5: Controller]
```

### Bước 1: Ánh xạ Entity (`entity/`)
* Tạo class tương ứng với bảng trong CSDL.
* Đặt tên Class dạng PascalCase (`BenhNhan.java`), tên bảng trong `@Table(name = "benhnhan")`.
* Khóa chính dùng `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
* Quan hệ: Dùng `@ManyToOne(fetch = FetchType.LAZY)` hoặc `@OneToOne(fetch = FetchType.LAZY)`.

### Bước 2: Tạo Repository (`repository/`)
* Tạo interface kế thừa `JpaRepository<Entity, PrimaryKeyType>`.
* Khai báo các query method theo chuẩn của Spring Data JPA (ví dụ: `findByMaHis(...)`, `existsBySoDienThoai(...)`).

### Bước 3: Tạo DTO (`dto/request/` và `dto/response/`)
* `XxxRequest.java`: Dùng validation như `@NotBlank`, `@NotNull`, `@Pattern`, `@Size`.
* `XxxResponse.java`: Chỉ chứa các trường cần thiết trả về cho giao diện.

### Bước 4: Viết Service (`service/`)
* Viết logic nghiệp vụ, kiểm tra ràng buộc, xử lý tính toán.
* Nếu có lỗi nghiệp vụ: `throw new RuntimeException("Mô tả lỗi tiếng Việt rõ ràng")`.

### Bước 5: Viết Controller (`controller/`)
* Định nghĩa endpoint rõ ràng: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`.
* Nhớ gắn `@Valid` trước `@RequestBody` để kích hoạt validation.

---

## 🌿 3. QUY TRÌNH LÀM VIỆC VỚI GIT (GIT WORKFLOW)

**Quy tắc:** Tuyệt đối KHÔNG commit trực tiếp lên nhánh `main` hoặc `develop`. Mỗi thành viên khi làm tính năng phải tạo một nhánh riêng.

### Quy ước đặt tên nhánh:
* Tính năng mới: `feature/<tên-tính-năng>` (Ví dụ: `feature/ho-so-benh-an`, `feature/don-thuoc`, `feature/uas7`)
* Sửa lỗi: `fix/<tên-lỗi>` (Ví dụ: `fix/login-lazy-loading`)

---

### 📝 Hướng dẫn từng lệnh Git chi tiết cho thành viên:

#### 1. Lấy code mới nhất về máy trước khi làm:
Mở Terminal và gõ:
```bash
git checkout develop
git pull origin develop
```

#### 2. Tạo một nhánh mới của riêng bạn và chuyển sang nhánh đó:
```bash
git checkout -b feature/ten-tinh-nang-cua-ban
```
*(Ví dụ: `git checkout -b feature/quan-ly-benh-nhan`)*

#### 3. Viết code & Kiểm tra chạy thử:
* Tiến hành code theo pipeline 5 bước ở trên.
* Chạy thử trên máy bằng lệnh: `.\mvnw spring-boot:run`.
* Mở Postman test đảm bảo API chạy đúng, trả về mã `200 OK`.

#### 4. Lưu lại các thay đổi và Commit:
Kiểm tra xem mình đã sửa những file nào:
```bash
git status
```
Đưa tất cả file thay đổi vào danh sách chuẩn bị lưu:
```bash
git add .
```
Ghi lại mô tả việc bạn vừa làm (Commit message):
```bash
git commit -m "feat: them entity va api quan ly benh nhan"
```

#### 5. Đẩy nhánh của bạn lên GitHub/GitLab:
```bash
git push -u origin feature/ten-tinh-nang-cua-ban
```

#### 6. Tạo Pull Request (PR) để gộp code:
* Lên giao diện GitHub/GitLab của dự án.
* Bạn sẽ thấy thông báo bấm vào nút **Compare & create pull request**.
* Chọn gộp từ nhánh của bạn (`feature/...`) vào nhánh `develop`.
* Báo cho Leader hoặc thành viên khác Review trước khi merge code vào nhánh chung.

---

## 🔒 4. QUY TẮC BẢO VỆ CSDL
* Trong file `application.yml`, chế độ `spring.jpa.hibernate.ddl-auto` bắt buộc phải là **`validate`** (hoặc `none`).
* **TUYỆT ĐỐI KHÔNG ĐƯỢC ĐỔI** sang `update` hay `create` vì sẽ làm sai lệch hoặc xóa mất dữ liệu CSDL đã chốt của cả nhóm!

