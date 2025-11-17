# Tổng Quan Dự Án: Mạng Xã Hội Mini

## 1. Giới Thiệu

Đây là một ứng dụng mạng xã hội mini được xây dựng trên nền tảng Android sử dụng Kotlin và Jetpack Compose. Ứng dụng cho phép người dùng chia sẻ các bài đăng (post) dưới dạng văn bản và hình ảnh, tương tác với các bài đăng khác thông qua việc "thích" (like) và "bình luận" (comment), cũng như quản lý thông tin cá nhân của mình.

Dự án được thiết kế theo kiến trúc hiện đại, tập trung vào khả năng mở rộng, bảo trì và cung cấp trải nghiệm người dùng mượt mà với sự hỗ trợ offline và cập nhật dữ liệu real-time.

---

## 2. Các Công Nghệ và Kiến Trúc Sử Dụng

- **Ngôn ngữ:** Kotlin
- **Giao diện người dùng (UI):** Jetpack Compose & Material 3
- **Kiến trúc:** MVVM (Model-View-ViewModel) kết hợp với các nguyên tắc của Clean Architecture (UseCase, Repository).
- **Dependency Injection:** Hilt
- **Bất đồng bộ (Asynchronous):** Kotlin Coroutines & Flow
- **Backend & Dịch vụ đám mây (Cloud Services):**
    - **Firebase Authentication:** Quản lý đăng nhập, đăng ký bằng Email/Password.
    - **Cloud Firestore:** Cơ sở dữ liệu NoSQL để lưu trữ thông tin người dùng, bài đăng, lượt thích, và bình luận.
    - **Firebase Storage:** Lưu trữ các file media (hình ảnh bài đăng, ảnh đại diện).
- **Cơ sở dữ liệu cục bộ (Local Database):** Room Database để cache dữ liệu, hỗ trợ xem offline.
- **Phân trang (Paging):** Jetpack Paging 3 Library để tải dữ liệu vô hạn (infinite scrolling) một cách hiệu quả.
- **Quản lý tác vụ nền (Background Tasks):** WorkManager để xử lý việc tải lên bài đăng (hình ảnh và dữ liệu) một cách đáng tin cậy, ngay cả khi ứng dụng không chạy.
- **Điều hướng (Navigation):** Jetpack Navigation for Compose
- **Tải ảnh (Image Loading):** Coil
- **Đa ngôn ngữ (Multi-language):** Hỗ trợ Tiếng Việt và Tiếng Anh.

---

## 3. Luồng Hoạt Động (Application Flow)

### 3.1. Luồng Xác Thực (Authentication)

1.  **Màn hình khởi động (Splash Screen):** Kiểm tra trạng thái đăng nhập của người dùng.
    - **Đã đăng nhập:** Chuyển thẳng đến màn hình `FeedScreen`.
    - **Chưa đăng nhập:** Chuyển đến màn hình `LoginScreen`.
2.  **Màn hình Đăng nhập (LoginScreen):**
    - Người dùng nhập email và mật khẩu.
    - Có thể điều hướng đến `RegisterScreen`.
    - Khi đăng nhập thành công, chuyển đến `FeedScreen` và xóa toàn bộ lịch sử điều hướng cũ.
3.  **Màn hình Đăng ký (RegisterScreen):**
    - Người dùng nhập tên, email, mật khẩu.
    - Khi đăng ký thành công, tự động đăng nhập và chuyển đến `FeedScreen`.

### 3.2. Luồng Chính (Main App)

1.  **Màn hình Feed (FeedScreen):**
    - Hiển thị danh sách các bài đăng từ tất cả người dùng theo thứ tự thời gian mới nhất.
    - **Tải vô hạn (Infinite Scroll):** Tự động tải thêm bài đăng cũ khi người dùng cuộn xuống cuối danh sách.
    - **Kéo để làm mới (Pull to Refresh):** Tải lại các bài đăng mới nhất.
    - **Tương tác:**
        - Click vào một bài đăng để đến `PostDetailScreen`.
        - Click vào tên/avatar của tác giả để đến `ProfileScreen`.
        - Click vào nút "Thích" để tương tác.
    - **Điều hướng:**
        - Nút "+" để đến `ComposePostScreen`.
        - Menu để đến `SettingsScreen` (thay đổi ngôn ngữ, đăng xuất).

2.  **Màn hình Tạo Bài Đăng (ComposePostScreen):**
    - Nhập nội dung văn bản.
    - Chọn từ 1 đến 3 hình ảnh từ thư viện.
    - Khi nhấn "Đăng", ứng dụng sử dụng `WorkManager` để tải dữ liệu lên Firebase trong nền.
    - Sau khi bắt đầu tác vụ tải lên, người dùng được đưa trở lại `FeedScreen` và màn hình này sẽ tự động làm mới để hiển thị bài đăng mới.

3.  **Màn hình Chi tiết Bài Đăng (PostDetailScreen):**
    - Hiển thị nội dung chi tiết của một bài đăng và danh sách các bình luận.
    - **Tương tác:**
        - Thêm bình luận mới.
        - Xóa bài đăng (nếu là chủ bài đăng).
        - Xóa bình luận (nếu là chủ bài đăng hoặc chủ bình luận).
        - Click vào tên/avatar người bình luận để đến `ProfileScreen`.

4.  **Màn hình Hồ sơ (ProfileScreen):**
    - Hiển thị thông tin người dùng (avatar, tên) và danh sách các bài đăng của họ.
    - **Điều hướng:**
        - Nút "Chỉnh sửa" để đến `EditProfileScreen`.
        - Click vào một bài đăng để xem chi tiết tại `PostDetailScreen`.

5.  **Màn hình Chỉnh sửa Hồ sơ (EditProfileScreen):**
    - Cho phép người dùng thay đổi tên hiển thị và ảnh đại diện.
    - Sau khi lưu, thông tin mới sẽ được cập nhật lên Firebase.
    - **Cập nhật toàn cục:** Một cơ chế được kích hoạt để tất cả các màn hình (`Feed`, `Profile`, `PostDetail`) tự động cập nhật và hiển thị thông tin mới (tên, avatar) mà không cần người dùng phải làm mới thủ công.

### 3.3. Luồng Dữ Liệu và Đồng Bộ

1.  **Tải Dữ Liệu (Paging 3 & RemoteMediator):**
    - Khi người dùng mở `FeedScreen`, `Paging 3` sẽ yêu cầu dữ liệu.
    - `RemoteMediator` được kích hoạt. Nó sẽ kiểm tra xem có cần lấy dữ liệu mới từ Firestore hay không.
    - Nếu cần, nó sẽ fetch dữ liệu từ Firestore, lưu vào Room Database (cache), sau đó `Paging 3` sẽ đọc từ Room để hiển thị lên UI.
    - Quá trình này đảm bảo UI luôn hiển thị dữ liệu từ cache trước tiên (load nhanh, hỗ trợ offline), sau đó mới cập nhật từ mạng.

2.  **Tạo/Sửa/Xóa (Create/Update/Delete):**
    - **Tạo bài đăng:** Dùng `WorkManager` để đảm bảo việc tải lên thành công. Sau khi thành công, `FeedScreen` được thông báo để làm mới.
    - **Cập nhật hồ sơ:** Dữ liệu được ghi trực tiếp lên Firestore. Một `SavedStateHandle` được sử dụng trong `NavGraph` để truyền tín hiệu "cần làm mới" đến các màn hình liên quan (`FeedScreen`, `ProfileScreen`).
    - **Xóa bài đăng/bình luận:**
        - Một Cloud Function (hoặc logic phía client) được kích hoạt để xóa các dữ liệu phụ thuộc (ví dụ: xóa bình luận, lượt thích khi xóa bài đăng).
        - Sau khi xóa thành công trên server, màn hình (`FeedScreen` hoặc `PostDetailScreen`) sẽ được làm mới để loại bỏ mục đã xóa khỏi UI và cache.

3.  **Hỗ trợ Offline:**
    - Nhờ có Room cache, người dùng vẫn có thể xem các bài đăng đã được tải trước đó ngay cả khi không có kết nối mạng.
    - Mọi hành động tạo/sửa/xóa sẽ được Firestore SDK tự động đưa vào hàng đợi và thực hiện khi có kết nối mạng trở lại.

---

