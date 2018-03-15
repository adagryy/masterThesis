import UIKit
import AVFoundation
import Alamofire

class TakingImageViewController: UIViewController, UIImagePickerControllerDelegate, UINavigationControllerDelegate, UIScrollViewDelegate, UIPickerViewDelegate, UIPickerViewDataSource {

    private var newImageView = UIImageView()
    private var image: UIImage! {
        didSet {
            imageView.contentMode = .scaleAspectFit
            imageView.image = image
        }
    }
    
    private var pickerDataSource: [String] = [] {
        didSet {
            pickerView.reloadAllComponents()
        }
    }
    private var pickedRow = 0
    
    @IBOutlet weak var imageView: UIImageView!
    @IBOutlet weak var pickerView: UIPickerView!
    @IBOutlet weak var activityIndicator: UIActivityIndicatorView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.imageView.isUserInteractionEnabled = false
        self.pickerView.dataSource = self
        self.pickerView.delegate = self
        
        Alamofire.request(
            URL(string: serverAddresses.getAlgorithmsAddress)!,
            method: .post,
            encoding: JSONEncoding.default).validate().responseJSON { (response) in
                if let values = response.result.value {
                    if let json = values as? NSDictionary {
                        for (_, value) in json {
                            if let tmp = value as? String {
                                self.pickerDataSource.append(tmp)
                            }
                        }
                    }
                }
        }
    }
    
    @IBAction func takingPhoto(_ sender: UIButton) {
        let picker = UIImagePickerController()
        picker.delegate = self
        picker.allowsEditing = false
        picker.sourceType = UIImagePickerControllerSourceType.camera
        picker.cameraCaptureMode = .photo
        picker.modalPresentationStyle = .fullScreen
        present(picker, animated: true, completion: nil)
        // popracować nad dostępem
        /*if UIImagePickerController.isSourceTypeAvailable(.camera) {
            if AVCaptureDevice.authorizationStatus(for: .video) == .authorized {
                
            } else {
                noAccessAlert()
            }
        } else {
            noCameraAlert()
        }*/
    }
    
    @IBAction func pickingPhotoFromLibrary(_ sender: UIButton) {
        let picker = UIImagePickerController()
        picker.delegate = self
        picker.allowsEditing = false
        picker.sourceType = UIImagePickerControllerSourceType.photoLibrary
        picker.mediaTypes = UIImagePickerController.availableMediaTypes(for: .photoLibrary)!
        present(picker, animated: true, completion: nil)
    }
    
    @IBAction func imageTapped(_ sender: UITapGestureRecognizer) {
        let scrollView = UIScrollView()
        scrollView.delegate = self
        scrollView.frame = UIScreen.main.bounds
        scrollView.backgroundColor = .black
        scrollView.minimumZoomScale = 1.0
        scrollView.maximumZoomScale = 10.0
        
        let imageView = sender.view as? UIImageView
        newImageView.image = imageView?.image
        newImageView.clipsToBounds = true
        newImageView.contentMode = .scaleAspectFit
        newImageView.backgroundColor = .black
        newImageView.frame = UIScreen.main.bounds
        scrollView.addSubview(newImageView)
        
        let tap = UITapGestureRecognizer(target: self, action: #selector(dismissFullScreenImage))
        scrollView.addGestureRecognizer(tap)
        
        self.view.addSubview(scrollView)
        self.navigationController?.isNavigationBarHidden = true
        self.tabBarController?.tabBar.isHidden = true
    }
    
    @IBAction func sendImageToProcess(_ sender: UIButton) {
        popLoadingView()
        if let image = image {
            let imageData = UIImageJPEGRepresentation(image, 0.5)!
            let parameters = ["selectedAlgorithm": pickerDataSource[pickedRow]];
            
            Alamofire.upload(multipartFormData: { (multipartFormData) in
                //multipartFormData.append(imageData, withName: "selectedImage")
                multipartFormData.append(imageData, withName: "selectedImage", fileName: "file.jpg", mimeType: "image/jpeg")
                for (key, value) in parameters {
                    multipartFormData.append(value.data(using: String.Encoding.utf8)!, withName: key)
                }
            }, to: serverAddresses.imageUploading,
               encodingCompletion: { (result) in
                switch result {
                case .success(let upload, _, _):
                    //upload.uploadProgress(closure: { (progress) in
                    //    print("Upload Progress: \(progress.fractionCompleted)")
                    //})
                    // kod odpowiedzi
                    upload.responseJSON { response in
                        if let code = response.response?.statusCode {
                            if code == 200 {
                                self.removeLoadingView()
                                self.popsTheAlert(title: "OK", message: "Zdjęcie wysłane prawidłowo")
                            } else {
                                self.removeLoadingView()
                                self.popsTheAlert(title: "Błąd", message: "Błąd w trakcie przetwarzania danych. Prosimy spróbować ponownie.")
                            }
                        }
                    }
                case .failure:
                    self.removeLoadingView()
                    self.popsTheAlert(title: "Błąd", message: "Błąd w trakcie przesyłania danych. Prosimy spróbować ponownie.")
                }
            })
        }
    }
    
    @IBAction func receiveImageFromProcessing(_ sender: UIButton) {
        popLoadingView()
        Alamofire.request(
            URL(string: serverAddresses.receiveImage)!,
            method: .post,
            encoding: JSONEncoding.default).validate().responseJSON { (response) in
                if let responseCode = response.response?.statusCode {
                    switch responseCode {
                    case 200:
                        self.removeLoadingView()
                        self.popsTheAlert(title: "OK", message: "Odbiór zdjęcia zakończony prawidłowo")
                        if let data = response.data {
                            self.image = UIImage(data: data, scale: 1)
                        }
                    default:
                        self.removeLoadingView()
                        self.popsTheAlert(title: "Błąd", message: "Błąd w trakcie pobierania zdjęcia. Prosimy spróbować ponownie.")
                    }
                }
        }
    }
    
    @objc func dismissFullScreenImage(sender: UITapGestureRecognizer) {
        self.navigationController?.isNavigationBarHidden = false
        self.tabBarController?.tabBar.isHidden = false
        sender.view?.removeFromSuperview()
    }
    
    private func noCameraAlert() {
        let alert = UIAlertController(title: "Brak aparatu", message: "Urządzenie którego używasz nie posiada aparatu lub jest on zepsuty", preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK", style: .default, handler: nil)
        alert.addAction(okAction)
        self.present(alert, animated: true, completion: nil)
    }
    
    private func noAccessAlert() {
        let alert = UIAlertController(title: "Brak dostępu", message: "Kamera jest wymagana do zrobienia zdjęcia próbki", preferredStyle: .alert)
        let cancelAction = UIAlertAction(title: "Anuluj", style: .cancel, handler: nil)
        let givePermissionAction = UIAlertAction(title: "Daj dostęp", style: .default, handler: { (alert) in
            UIApplication.shared.open(URL(string: UIApplicationOpenSettingsURLString)!, options: [:], completionHandler: nil)
        })
        alert.addAction(cancelAction)
        alert.addAction(givePermissionAction)
        self.present(alert, animated: true, completion: nil)
    }
    
    // Mark: - Delegates
    // picker controller
    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        dismiss(animated: true, completion: nil)
    }
    
    func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [String : Any]) {
        image = info[UIImagePickerControllerOriginalImage] as! UIImage
        imageView.isUserInteractionEnabled = true
        dismiss(animated: true, completion: nil)
    }
    
    // scroll view
    func viewForZooming(in scrollView: UIScrollView) -> UIView? {
        return newImageView
    }
    
    func numberOfComponents(in pickerView: UIPickerView) -> Int {
        return 1
    }
    
    // picker view
    func pickerView(_ pickerView: UIPickerView, numberOfRowsInComponent component: Int) -> Int {
        return pickerDataSource.count
    }
    
    func pickerView(_ pickerView: UIPickerView, titleForRow row: Int, forComponent component: Int) -> String? {
        return pickerDataSource[row]
    }
    
    func pickerView(_ pickerView: UIPickerView, didSelectRow row: Int, inComponent component: Int) {
        pickedRow = row
    }
}
